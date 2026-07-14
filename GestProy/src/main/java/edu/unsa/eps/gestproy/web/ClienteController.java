package edu.unsa.eps.gestproy.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import edu.unsa.eps.gestproy.model.Cliente;
import edu.unsa.eps.gestproy.model.referencial.ReferencialTabla;
import edu.unsa.eps.gestproy.service.ClienteService;
import edu.unsa.eps.gestproy.service.ReferencialService;

@Controller
@RequestMapping("/clientes")
public class ClienteController extends MantenimientoControllerBase {

    private final ClienteService service;
    private final ReferencialService referenciales;

    public ClienteController(ClienteService service, ReferencialService referenciales) {
        this.service = service;
        this.referenciales = referenciales;
    }

    /** Catálogos activos para los <select> del formulario. */
    private void cargarCatalogos(Model model) {
        model.addAttribute("tiposCliente",
            referenciales.listar(ReferencialTabla.TIP_CLI).stream()
                .filter(r -> "A".equals(r.getEstReg())).toList());
        model.addAttribute("estadosCliente",
            referenciales.listar(ReferencialTabla.EST_CLI).stream()
                .filter(r -> "A".equals(r.getEstReg())).toList());
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("clientes", service.listar());
        return "clientes/list";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("cliente", new Cliente());
        model.addAttribute("esEdicion", false);
        cargarCatalogos(model);
        return "clientes/form";
    }

    @PostMapping
    public String adicionar(@ModelAttribute("cliente") Cliente cliente, RedirectAttributes ra) {
        boolean ok = ejecutar(() -> service.adicionar(cliente),
                "Cliente adicionado correctamente", ra);
        return ok ? "redirect:/clientes" : "redirect:/clientes/nuevo";
    }

    @GetMapping("/{cod}/editar")
    public String editar(@PathVariable int cod, Model model) {
        Cliente cliente = service.buscar(cod);
        if (cliente == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el cliente " + cod);
        }
        model.addAttribute("cliente", cliente);
        model.addAttribute("esEdicion", true);
        cargarCatalogos(model);
        return "clientes/form";
    }

    @PostMapping("/{cod}")
    public String modificar(@PathVariable int cod,
                            @ModelAttribute("cliente") Cliente cliente,
                            RedirectAttributes ra) {
        cliente.setCod(cod);
        boolean ok = ejecutar(() -> service.modificar(cliente),
                "Cliente modificado correctamente", ra);
        return ok ? "redirect:/clientes" : "redirect:/clientes/" + cod + "/editar";
    }

    @PostMapping("/{cod}/eliminar")
    public String eliminar(@PathVariable int cod, RedirectAttributes ra) {
        ejecutar(() -> service.cambiarEstado(cod, "ELIMINAR"), "Cliente eliminado (lógicamente)", ra);
        return "redirect:/clientes";
    }

    @PostMapping("/{cod}/inactivar")
    public String inactivar(@PathVariable int cod, RedirectAttributes ra) {
        ejecutar(() -> service.cambiarEstado(cod, "INACTIVAR"), "Cliente inactivado", ra);
        return "redirect:/clientes";
    }

    @PostMapping("/{cod}/reactivar")
    public String reactivar(@PathVariable int cod, RedirectAttributes ra) {
        ejecutar(() -> service.cambiarEstado(cod, "REACTIVAR"), "Cliente reactivado", ra);
        return "redirect:/clientes";
    }
}
