package edu.unsa.eps.gestproy.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import edu.unsa.eps.gestproy.dao.UsuarioDao;
import edu.unsa.eps.gestproy.security.CookieUtil;
import edu.unsa.eps.gestproy.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Login/logout de la única cuenta con permiso de escritura ("admi").
 * No hay registro de usuarios ni gestión de cuentas: solo existe
 * esta, y su contraseña se verifica contra el hash de g1s_usuario
 * (fn_usuario_autenticar) sin que la contraseña en texto plano
 * salga nunca de esa función SQL.
 */
@Controller
public class AuthController {

    private final UsuarioDao usuarioDao;
    private final JwtService jwtService;
    private final CookieUtil cookieUtil;

    public AuthController(UsuarioDao usuarioDao, JwtService jwtService, CookieUtil cookieUtil) {
        this.usuarioDao = usuarioDao;
        this.jwtService = jwtService;
        this.cookieUtil = cookieUtil;
    }

    @GetMapping("/login")
    public String formulario() {
        return "login";
    }

    @PostMapping("/login")
    public String iniciarSesion(@RequestParam String login, @RequestParam String password,
                                 HttpServletResponse response, RedirectAttributes ra) {
        if (!usuarioDao.autenticar(login, password)) {
            ra.addFlashAttribute("error", "Usuario o contraseña incorrectos");
            return "redirect:/login";
        }
        cookieUtil.escribirAccessToken(response, jwtService.generarAccessToken(login),
                jwtService.accessMaxAgeSegundos());
        cookieUtil.escribirRefreshToken(response, jwtService.generarRefreshToken(login),
                jwtService.refreshMaxAgeSegundos());
        ra.addFlashAttribute("exito", "Sesión iniciada correctamente");
        return "redirect:/";
    }

    @PostMapping("/logout")
    public String cerrarSesion(HttpServletResponse response, RedirectAttributes ra) {
        cookieUtil.borrarCookiesSesion(response);
        ra.addFlashAttribute("exito", "Sesión cerrada");
        return "redirect:/";
    }
}
