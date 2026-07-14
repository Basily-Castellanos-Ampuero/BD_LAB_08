package edu.unsa.eps.gestproy.web;

import org.springframework.dao.DataAccessException;
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

import edu.unsa.eps.gestproy.exception.ErroresBd;
import edu.unsa.eps.gestproy.model.Proyecto;
import edu.unsa.eps.gestproy.model.referencial.ReferencialTabla;
import edu.unsa.eps.gestproy.service.ClienteService;
import edu.unsa.eps.gestproy.service.ProyectoService;
import edu.unsa.eps.gestproy.service.ReferencialService;

/**
 * Gestión de proyectos: cabecera (crear/editar/cambiar estado)
 * y equipo asignado. La PK compuesta viaja en la URL como
 * /proyectos/{cliCod}/{tipCod}/{sec}.
 */
@Controller
@RequestMapping("/proyectos")
public class ProyectoController extends MantenimientoControllerBase {

    private final ProyectoService service;
    private final ClienteService clientes;
    private final ReferencialService referenciales;

    public ProyectoController(ProyectoService service, ClienteService clientes,
                              ReferencialService referenciales) {
        this.service = service;
        this.clientes = clientes;
        this.referenciales = referenciales;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("proyectos", service.listar());
        return "proyectos/list";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("proyecto", new Proyecto());
        model.addAttribute("esEdicion", false);
        cargarCatalogosForm(model);
        return "proyectos/form";
    }

    @PostMapping
    public String crear(@ModelAttribute("proyecto") Proyecto proyecto, RedirectAttributes ra) {
        try {
            int sec = service.crear(proyecto);
            ra.addFlashAttribute("exito",
                "Proyecto creado con secuencia " + sec + " para el cliente " + proyecto.getCliCod());
            return "redirect:/proyectos/" + proyecto.getCliCod() + "/" + proyecto.getTipCod() + "/" + sec;
        } catch (DataAccessException ex) {
            ra.addFlashAttribute("error", ErroresBd.extraerMensaje(ex));
            return "redirect:/proyectos/nuevo";
        }
    }

    @GetMapping("/{cliCod}/{tipCod}/{sec}")
    public String detalle(@PathVariable int cliCod, @PathVariable int tipCod, @PathVariable int sec,
                          Model model) {
        Proyecto proyecto = buscarObligatorio(cliCod, tipCod, sec);
        model.addAttribute("proyecto", proyecto);
        model.addAttribute("equipo", service.equipo(cliCod, tipCod, sec));
        model.addAttribute("estadosProyecto",
            referenciales.listar(ReferencialTabla.EST_PRO).stream()
                .filter(r -> "A".equals(r.getEstReg())).toList());
        return "proyectos/detalle";
    }

    @GetMapping("/{cliCod}/{tipCod}/{sec}/editar")
    public String editar(@PathVariable int cliCod, @PathVariable int tipCod, @PathVariable int sec,
                         Model model) {
        model.addAttribute("proyecto", buscarObligatorio(cliCod, tipCod, sec));
        model.addAttribute("esEdicion", true);
        cargarCatalogosForm(model);
        return "proyectos/form";
    }

    @PostMapping("/{cliCod}/{tipCod}/{sec}")
    public String modificar(@PathVariable int cliCod, @PathVariable int tipCod, @PathVariable int sec,
                            @ModelAttribute("proyecto") Proyecto proyecto, RedirectAttributes ra) {
        proyecto.setCliCod(cliCod);
        proyecto.setTipCod(tipCod);
        proyecto.setSec(sec);
        boolean ok = ejecutar(() -> service.editar(proyecto), "Proyecto modificado correctamente", ra);
        String base = "/proyectos/" + cliCod + "/" + tipCod + "/" + sec;
        return "redirect:" + (ok ? base : base + "/editar");
    }

    @PostMapping("/{cliCod}/{tipCod}/{sec}/estado")
    public String cambiarEstado(@PathVariable int cliCod, @PathVariable int tipCod, @PathVariable int sec,
                                @RequestParam String nuevoEstado, RedirectAttributes ra) {
        ejecutar(() -> service.cambiarEstado(cliCod, tipCod, sec, nuevoEstado),
                "Estado del proyecto actualizado", ra);
        return "redirect:/proyectos/" + cliCod + "/" + tipCod + "/" + sec;
    }

    // ---- equipo ----

    @GetMapping("/{cliCod}/{tipCod}/{sec}/equipo")
    public String equipo(@PathVariable int cliCod, @PathVariable int tipCod, @PathVariable int sec,
                         Model model) {
        model.addAttribute("proyecto", buscarObligatorio(cliCod, tipCod, sec));
        model.addAttribute("equipo", service.equipo(cliCod, tipCod, sec));
        model.addAttribute("disponibles", service.disponibles(cliCod, tipCod, sec));
        return "proyectos/equipo";
    }

    @PostMapping("/{cliCod}/{tipCod}/{sec}/equipo")
    public String asignar(@PathVariable int cliCod, @PathVariable int tipCod, @PathVariable int sec,
                          @RequestParam String asignacion, RedirectAttributes ra) {
        // el <select> envía "perCod|carProCod" (PK compuesta de la autorización)
        String[] partes = asignacion.split("\\|");
        int perCod = Integer.parseInt(partes[0]);
        int carProCod = Integer.parseInt(partes[1]);
        ejecutar(() -> service.asignarEquipo(cliCod, tipCod, sec, perCod, carProCod),
                "Persona asignada al equipo", ra);
        return "redirect:/proyectos/" + cliCod + "/" + tipCod + "/" + sec + "/equipo";
    }

    @PostMapping("/{cliCod}/{tipCod}/{sec}/equipo/{perCod}/{carProCod}/quitar")
    public String quitar(@PathVariable int cliCod, @PathVariable int tipCod, @PathVariable int sec,
                         @PathVariable int perCod, @PathVariable int carProCod, RedirectAttributes ra) {
        ejecutar(() -> service.quitarEquipo(cliCod, tipCod, sec, perCod, carProCod),
                "Persona retirada del equipo (baja lógica)", ra);
        return "redirect:/proyectos/" + cliCod + "/" + tipCod + "/" + sec + "/equipo";
    }

    @PostMapping("/{cliCod}/{tipCod}/{sec}/equipo/{perCod}/{carProCod}/reactivar")
    public String reactivar(@PathVariable int cliCod, @PathVariable int tipCod, @PathVariable int sec,
                            @PathVariable int perCod, @PathVariable int carProCod, RedirectAttributes ra) {
        ejecutar(() -> service.reactivarEquipo(cliCod, tipCod, sec, perCod, carProCod),
                "Persona reincorporada al equipo", ra);
        return "redirect:/proyectos/" + cliCod + "/" + tipCod + "/" + sec + "/equipo";
    }

    // ---- soporte ----

    private Proyecto buscarObligatorio(int cliCod, int tipCod, int sec) {
        Proyecto proyecto = service.buscar(cliCod, tipCod, sec);
        if (proyecto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No existe el proyecto (" + cliCod + ", " + tipCod + ", " + sec + ")");
        }
        return proyecto;
    }

    private void cargarCatalogosForm(Model model) {
        model.addAttribute("clientesActivos",
            clientes.listar().stream().filter(c -> "A".equals(c.getEstReg())).toList());
        model.addAttribute("tiposProyecto",
            referenciales.listar(ReferencialTabla.TIP_PRO).stream()
                .filter(r -> "A".equals(r.getEstReg())).toList());
    }
}
