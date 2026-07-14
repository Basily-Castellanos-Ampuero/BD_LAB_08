package edu.unsa.eps.gestproy.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import edu.unsa.eps.gestproy.model.Personal;
import edu.unsa.eps.gestproy.model.referencial.ReferencialTabla;
import edu.unsa.eps.gestproy.service.PersonalService;
import edu.unsa.eps.gestproy.service.ReferencialService;

@Controller
@RequestMapping("/personal")
public class PersonalController extends MantenimientoControllerBase {

    private final PersonalService service;
    private final ReferencialService referenciales;

    public PersonalController(PersonalService service, ReferencialService referenciales) {
        this.service = service;
        this.referenciales = referenciales;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("personal", service.listar());
        return "personal/list";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("persona", new Personal());
        model.addAttribute("esEdicion", false);
        cargarCargosPersonal(model);
        return "personal/form";
    }

    @PostMapping
    public String adicionar(@ModelAttribute("persona") Personal persona, RedirectAttributes ra) {
        boolean ok = ejecutar(() -> service.adicionar(persona),
                "Personal adicionado correctamente", ra);
        return ok ? "redirect:/personal" : "redirect:/personal/nuevo";
    }

    @GetMapping("/{cod}/editar")
    public String editar(@PathVariable int cod, Model model) {
        Personal persona = buscarObligatorio(cod);
        model.addAttribute("persona", persona);
        model.addAttribute("esEdicion", true);
        cargarCargosPersonal(model);
        return "personal/form";
    }

    @PostMapping("/{cod}")
    public String modificar(@PathVariable int cod,
                            @ModelAttribute("persona") Personal persona,
                            RedirectAttributes ra) {
        persona.setCod(cod);
        boolean ok = ejecutar(() -> service.modificar(persona),
                "Personal modificado correctamente", ra);
        return ok ? "redirect:/personal" : "redirect:/personal/" + cod + "/editar";
    }

    @PostMapping("/{cod}/eliminar")
    public String eliminar(@PathVariable int cod, RedirectAttributes ra) {
        ejecutar(() -> service.cambiarEstado(cod, "ELIMINAR"), "Personal eliminado (lógicamente)", ra);
        return "redirect:/personal";
    }

    @PostMapping("/{cod}/inactivar")
    public String inactivar(@PathVariable int cod, RedirectAttributes ra) {
        ejecutar(() -> service.cambiarEstado(cod, "INACTIVAR"), "Personal inactivado", ra);
        return "redirect:/personal";
    }

    @PostMapping("/{cod}/reactivar")
    public String reactivar(@PathVariable int cod, RedirectAttributes ra) {
        ejecutar(() -> service.cambiarEstado(cod, "REACTIVAR"), "Personal reactivado", ra);
        return "redirect:/personal";
    }

    // ---- autorizaciones de cargo de proyecto (g1c_per_car) ----

    @GetMapping("/{cod}/cargos")
    public String cargos(@PathVariable int cod, Model model) {
        Personal persona = buscarObligatorio(cod);
        model.addAttribute("persona", persona);
        model.addAttribute("cargos", service.listarCargos(cod));
        model.addAttribute("cargosProyecto",
            referenciales.listar(ReferencialTabla.CAR_PRO).stream()
                .filter(r -> "A".equals(r.getEstReg())).toList());
        return "personal/cargos";
    }

    @PostMapping("/{cod}/cargos")
    public String adicionarCargo(@PathVariable int cod,
                                 @RequestParam int carProCod,
                                 RedirectAttributes ra) {
        ejecutar(() -> service.adicionarCargo(cod, carProCod),
                "Cargo autorizado correctamente", ra);
        return "redirect:/personal/" + cod + "/cargos";
    }

    @PostMapping("/{cod}/cargos/{carProCod}/inactivar")
    public String inactivarCargo(@PathVariable int cod, @PathVariable int carProCod,
                                 RedirectAttributes ra) {
        ejecutar(() -> service.cambiarEstadoCargo(cod, carProCod, "INACTIVAR"),
                "Autorización inactivada", ra);
        return "redirect:/personal/" + cod + "/cargos";
    }

    @PostMapping("/{cod}/cargos/{carProCod}/reactivar")
    public String reactivarCargo(@PathVariable int cod, @PathVariable int carProCod,
                                 RedirectAttributes ra) {
        ejecutar(() -> service.cambiarEstadoCargo(cod, carProCod, "REACTIVAR"),
                "Autorización reactivada", ra);
        return "redirect:/personal/" + cod + "/cargos";
    }

    @PostMapping("/{cod}/cargos/{carProCod}/eliminar")
    public String eliminarCargo(@PathVariable int cod, @PathVariable int carProCod,
                                RedirectAttributes ra) {
        ejecutar(() -> service.cambiarEstadoCargo(cod, carProCod, "ELIMINAR"),
                "Autorización eliminada (lógicamente)", ra);
        return "redirect:/personal/" + cod + "/cargos";
    }

    private Personal buscarObligatorio(int cod) {
        Personal persona = service.buscar(cod);
        if (persona == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el personal " + cod);
        }
        return persona;
    }

    private void cargarCargosPersonal(Model model) {
        model.addAttribute("cargosPersonal",
            referenciales.listar(ReferencialTabla.CAR_PER).stream()
                .filter(r -> "A".equals(r.getEstReg())).toList());
    }
}
