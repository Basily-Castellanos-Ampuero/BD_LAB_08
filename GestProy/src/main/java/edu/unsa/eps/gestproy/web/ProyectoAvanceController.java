package edu.unsa.eps.gestproy.web;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import edu.unsa.eps.gestproy.model.Proyecto;
import edu.unsa.eps.gestproy.model.referencial.ReferencialTabla;
import edu.unsa.eps.gestproy.service.ProyectoAvanceService;
import edu.unsa.eps.gestproy.service.ProyectoService;
import edu.unsa.eps.gestproy.service.ReferencialService;

/**
 * Avance por etapas: registro de horas trabajadas (g1t_pro_mov)
 * y % de avance del proyecto (v_proyecto_avance).
 */
@Controller
@RequestMapping("/proyectos/{cliCod}/{tipCod}/{sec}/avance")
public class ProyectoAvanceController extends MantenimientoControllerBase {

    private final ProyectoAvanceService service;
    private final ProyectoService proyectos;
    private final ReferencialService referenciales;

    public ProyectoAvanceController(ProyectoAvanceService service, ProyectoService proyectos,
                                    ReferencialService referenciales) {
        this.service = service;
        this.proyectos = proyectos;
        this.referenciales = referenciales;
    }

    @GetMapping
    public String avance(@PathVariable int cliCod, @PathVariable int tipCod, @PathVariable int sec,
                         Model model) {
        Proyecto proyecto = proyectos.buscar(cliCod, tipCod, sec);
        if (proyecto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No existe el proyecto (" + cliCod + ", " + tipCod + ", " + sec + ")");
        }
        model.addAttribute("proyecto", proyecto);
        model.addAttribute("avance", service.resumen(cliCod, tipCod, sec));
        model.addAttribute("movimientos", service.movimientos(cliCod, tipCod, sec));
        // miembros activos del equipo para el <select> del formulario
        model.addAttribute("equipoActivo",
            proyectos.equipo(cliCod, tipCod, sec).stream()
                .filter(m -> "A".equals(m.estReg())).toList());
        model.addAttribute("etapasActivas",
            referenciales.listar(ReferencialTabla.ETP_PRO).stream()
                .filter(r -> "A".equals(r.getEstReg())).toList());
        model.addAttribute("hoy", LocalDate.now());
        return "proyectos/avance";
    }

    @PostMapping
    public String registrar(@PathVariable int cliCod, @PathVariable int tipCod, @PathVariable int sec,
                            @RequestParam String miembro,
                            @RequestParam int etpCod,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecReg,
                            @RequestParam int horTra,
                            @RequestParam int minTra,
                            RedirectAttributes ra) {
        // el <select> del equipo envía "perCod|carProCod"
        String[] partes = miembro.split("\\|");
        int perCod = Integer.parseInt(partes[0]);
        int carProCod = Integer.parseInt(partes[1]);
        ejecutar(() -> service.registrar(cliCod, tipCod, sec, perCod, carProCod,
                                         etpCod, fecReg, horTra, minTra),
                "Avance registrado correctamente", ra);
        return "redirect:/proyectos/" + cliCod + "/" + tipCod + "/" + sec + "/avance";
    }
}
