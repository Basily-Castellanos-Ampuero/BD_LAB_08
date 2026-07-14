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

import edu.unsa.eps.gestproy.model.referencial.ReferencialTabla;
import edu.unsa.eps.gestproy.model.referencial.RegistroReferencial;
import edu.unsa.eps.gestproy.service.ReferencialService;

/**
 * Controlador genérico de mantenimiento de los 9 catálogos.
 * Equivalencia con el patrón Swing del docente:
 *   Adicionar -> GET /nuevo + POST /
 *   Modificar -> GET /{cod}/editar + POST /{cod}
 *   Eliminar/Inactivar/Reactivar -> POST /{cod}/{accion} desde el listado
 *   Actualizar -> el submit del formulario; Cancelar -> enlace al listado.
 */
@Controller
@RequestMapping("/referenciales/{tabla}")
public class ReferencialController extends MantenimientoControllerBase {

    private final ReferencialService service;

    public ReferencialController(ReferencialService service) {
        this.service = service;
    }

    @ModelAttribute("tabla")
    public ReferencialTabla resolverTabla(@PathVariable("tabla") String slug) {
        ReferencialTabla tabla = ReferencialTabla.porSlug(slug);
        if (tabla == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Catálogo no reconocido: " + slug);
        }
        return tabla;
    }

    @GetMapping
    public String listar(@ModelAttribute("tabla") ReferencialTabla tabla, Model model) {
        model.addAttribute("registros", service.listar(tabla));
        return "referenciales/list";
    }

    @GetMapping("/nuevo")
    public String nuevo(@ModelAttribute("tabla") ReferencialTabla tabla, Model model) {
        model.addAttribute("registro", new RegistroReferencial());
        model.addAttribute("esEdicion", false);
        return "referenciales/form";
    }

    @PostMapping
    public String adicionar(@ModelAttribute("tabla") ReferencialTabla tabla,
                            @ModelAttribute("registro") RegistroReferencial registro,
                            RedirectAttributes ra) {
        boolean ok = ejecutar(() -> service.adicionar(tabla, registro),
                tabla.getEtiqueta() + " adicionado correctamente", ra);
        return ok ? "redirect:/referenciales/" + tabla.getSlug()
                  : "redirect:/referenciales/" + tabla.getSlug() + "/nuevo";
    }

    @GetMapping("/{cod}/editar")
    public String editar(@ModelAttribute("tabla") ReferencialTabla tabla,
                         @PathVariable String cod, Model model) {
        RegistroReferencial registro = service.buscar(tabla, cod);
        if (registro == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No existe el registro " + cod + " en " + tabla.getEtiquetaPlural());
        }
        model.addAttribute("registro", registro);
        model.addAttribute("esEdicion", true);
        return "referenciales/form";
    }

    @PostMapping("/{cod}")
    public String modificar(@ModelAttribute("tabla") ReferencialTabla tabla,
                            @PathVariable String cod,
                            @ModelAttribute("registro") RegistroReferencial registro,
                            RedirectAttributes ra) {
        registro.setCod(cod);
        boolean ok = ejecutar(() -> service.modificar(tabla, registro),
                tabla.getEtiqueta() + " modificado correctamente", ra);
        return ok ? "redirect:/referenciales/" + tabla.getSlug()
                  : "redirect:/referenciales/" + tabla.getSlug() + "/" + cod + "/editar";
    }

    @PostMapping("/{cod}/eliminar")
    public String eliminar(@ModelAttribute("tabla") ReferencialTabla tabla,
                           @PathVariable String cod, RedirectAttributes ra) {
        ejecutar(() -> service.cambiarEstado(tabla, cod, "ELIMINAR"),
                tabla.getEtiqueta() + " eliminado (lógicamente)", ra);
        return "redirect:/referenciales/" + tabla.getSlug();
    }

    @PostMapping("/{cod}/inactivar")
    public String inactivar(@ModelAttribute("tabla") ReferencialTabla tabla,
                            @PathVariable String cod, RedirectAttributes ra) {
        ejecutar(() -> service.cambiarEstado(tabla, cod, "INACTIVAR"),
                tabla.getEtiqueta() + " inactivado", ra);
        return "redirect:/referenciales/" + tabla.getSlug();
    }

    @PostMapping("/{cod}/reactivar")
    public String reactivar(@ModelAttribute("tabla") ReferencialTabla tabla,
                            @PathVariable String cod, RedirectAttributes ra) {
        ejecutar(() -> service.cambiarEstado(tabla, cod, "REACTIVAR"),
                tabla.getEtiqueta() + " reactivado", ra);
        return "redirect:/referenciales/" + tabla.getSlug();
    }
}
