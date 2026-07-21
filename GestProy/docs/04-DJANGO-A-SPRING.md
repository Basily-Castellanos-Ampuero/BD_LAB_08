# GestProy — De Django a Spring Boot

> Para quien viene de Django y necesita entender GestProy sin primero
> aprender Spring Boot desde cero. Es más detallado que
> [`00-RESUMEN.md`](00-RESUMEN.md), pero su objetivo es distinto: no
> explica GestProy en el vacío, lo explica **por comparación** con las
> piezas de Django que ya conoces. Cuando algo no tiene equivalente
> directo (porque Spring Boot no lo necesita, o porque GestProy
> deliberadamente no lo usa), te lo digo explícitamente en vez de forzar
> una analogía que no existe.
>
> Si después de esto quieres el detalle método por método, sigue con
> [`01-METODOS-Y-LOGICA.md`](01-METODOS-Y-LOGICA.md); si quieres el porqué
> de cada decisión de diseño, [`03-DOCUMENTACION-GENERAL.md`](03-DOCUMENTACION-GENERAL.md).

---

## 1. La diferencia que más te va a chocar: no hay ORM

Esto es lo primero que hay que soltar mentalmente. En Django escribes:

```python
class Cliente(models.Model):
    nombre = models.CharField(max_length=60)
    tipo = models.ForeignKey(TipoCliente, on_delete=models.PROTECT)

    def save(self, *args, **kwargs):
        # aquí podrías poner validaciones antes de guardar
        super().save(*args, **kwargs)
```

y Django genera el `CREATE TABLE` por ti (`makemigrations`/`migrate`),
genera el `INSERT`/`UPDATE` por ti (`.save()`), y te deja meter lógica de
negocio en el propio modelo o en signals (`pre_save`, `post_save`).

**GestProy no tiene nada de eso, a propósito.** El curso de Base de Datos
exige que la lógica esté en SQL, no en el lenguaje de aplicación. Así que:

- Las tablas se crean con `CREATE TABLE` escrito a mano en archivos `.sql`
  versionados (`db/schema/`) — no hay `makemigrations`, no hay historial de
  migraciones automático. Si cambias una columna, editas el `.sql` y
  vuelves a aplicarlo (ver §5).
- No existe un `Cliente.objects.create(...)` que arme el `INSERT` por ti.
  En su lugar, Java llama a una función de PostgreSQL:
  `SELECT sp_cliente_mant('ADICIONAR', ?, ?, ?, ...)`, y **esa función**
  (escrita en PL/pgSQL, el "stored procedure" de PostgreSQL) hace el
  `INSERT` y valida todo.
- Las clases Java (`Cliente`, `Personal`, `Proyecto`...) son solo **moldes
  de datos** — el equivalente más cercano en Python no es un modelo
  Django, es más parecido a un `dataclass` o un `NamedTuple`: no saben
  guardarse a sí mismas, no tienen `.save()`, no tienen managers ni
  querysets. Alguien más (un `Dao`, ver §3) las llena leyendo un
  `ResultSet` a mano.

Dicho de otro modo: en Django, el ORM es la capa que sabe hablar con la
BD y tú casi nunca escribes SQL. En GestProy es al revés: **el SQL es la
capa que sabe todo**, y Java es apenas un mensajero.

---

## 2. Mapa rápido de conceptos

| En Django | En GestProy / Spring Boot | Diferencia clave |
|---|---|---|
| `models.py` (clase `Model`) | Tablas en `db/schema/*.sql` + clases Java en `model/` | Las clases Java **no** son modelos activos; son solo contenedores de datos. La tabla vive en SQL puro. |
| `makemigrations` / `migrate` | Ejecutar `db/scripts/apply-all.ps1` | No hay historial de migraciones automático ni detección de cambios; los `.sql` son la única fuente de verdad, aplicados manualmente y en orden. |
| `Model.objects.create(...)` / `.save()` | `jdbc.queryForObject("SELECT sp_x(...)", ...)` en un `Dao` | Nada arma el SQL de escritura por ti: siempre invocas una función PL/pgSQL explícita. |
| `views.py` (función o `class-based view`) | Clases `@Controller` en `web/` | Muy parecido conceptualmente: reciben la petición HTTP, llaman a la capa de negocio, devuelven qué renderizar. |
| `urls.py` centralizado | `@RequestMapping`/`@GetMapping`/`@PostMapping` **dentro** de cada controller | Spring no tiene un archivo central de rutas: cada clase declara sus propias rutas con anotaciones. |
| Templates Django (`{% extends %}`, `{% include %}`) | Thymeleaf (`th:replace`, fragmentos) | Mismo concepto (HTML renderizado en el servidor), sintaxis distinta — ver §6. |
| `forms.py` (`ModelForm`) | `@ModelAttribute` en el método del controller | Spring rellena un objeto Java con los campos del formulario automáticamente por nombre, sin una clase `Form` separada. |
| `settings.py` | `application.properties` + `db.properties` | `db.properties` es el equivalente a las variables de entorno/secrets que en Django cargarías con `django-environ` o `python-decouple`; está gitignored igual que harías con un `.env`. |
| `django.contrib.messages` (mensajes flash) | `RedirectAttributes.addFlashAttribute(...)` | Exactamente el mismo concepto: un mensaje que sobrevive un solo redirect. |
| Middleware (`MIDDLEWARE` en settings) | `Filter` (ej. `JwtAuthFilter`) | Mismo lugar en el ciclo de vida de la petición: corre antes de que la vista/controller se ejecute. |
| `@login_required` / `permission_classes` | `HandlerInterceptor` (`AutorizacionInterceptor`) | Django lo aplica por vista (decorador) o por ViewSet; GestProy lo aplica **globalmente** con una regla (todo POST, o GET a `/nuevo`/`/editar`) en vez de decorar cada controller uno por uno. |
| `request.user` | Atributo de request `gestproy.admin` + `@ModelAttribute("admin")` | Con una sola cuenta, GestProy no necesita un objeto usuario completo — solo un booleano "es admin o no". |
| Context processors (inyectan variables a *todos* los templates) | `@ControllerAdvice` con `@ModelAttribute` (`GlobalModelAttributes`) | Prácticamente idéntico: en Django, un context processor agrega `request.user` a cada template sin que cada vista lo pida; aquí, `GlobalModelAttributes` agrega `admin` a cada modelo sin que cada controller lo pida. |
| `django-rest-framework-simplejwt` (JWT en cookies) | `JwtService` + `CookieUtil` + `JwtAuthFilter` | Ver §7 — es la comparación más directa de todo este documento. |
| Apps de Django (`myapp/models.py`, `myapp/views.py`...) | Paquetes por **capa**: `model/`, `dao/`, `service/`, `web/` | Django organiza por *feature* (una carpeta = un dominio con su modelo+vista+admin); GestProy organiza por *capa* (una carpeta = un rol técnico, con clases de todos los dominios adentro). Es una diferencia de convención, no de framework. |
| `manage.py runserver` | `mvn spring-boot:run` | Equivalente directo. |
| Admin de Django (`django.contrib.admin`) | No existe | GestProy no tiene un admin autogenerado; cada pantalla de mantenimiento (`ReferencialController`, `ClienteController`...) está escrita a mano. |
| `pip` + `requirements.txt` | Maven + `pom.xml` | Maven además compila y empaqueta (Python no necesita ese paso). |

---

## 3. Cómo se ve una petición de punta a punta (comparado)

### En Django (con un ORM típico)

```python
# views.py
def crear_cliente(request):
    if request.method == "POST":
        form = ClienteForm(request.POST)
        if form.is_valid():
            form.save()              # el ORM arma el INSERT
            return redirect("clientes_lista")
    else:
        form = ClienteForm()
    return render(request, "clientes/form.html", {"form": form})
```

### En GestProy (sin ORM)

```java
// ClienteController.java
@PostMapping
public String adicionar(@ModelAttribute("cliente") Cliente cliente, RedirectAttributes ra) {
    boolean ok = ejecutar(() -> service.adicionar(cliente), "Cliente adicionado correctamente", ra);
    return ok ? "redirect:/clientes" : "redirect:/clientes/nuevo";
}
```
```java
// ClienteService.java — capa intermedia casi vacía (ver §2 de 00-RESUMEN.md)
public void adicionar(Cliente c) { dao.mantener("ADICIONAR", c); }
```
```java
// ClienteDao.java — aquí se toca la BD
public void mantener(String operacion, Cliente c) {
    jdbc.queryForObject(
        "SELECT sp_cliente_mant(?, ?, ?, ?::char(2), ?::char(1), ?::date, ?::date, ?::date)",
        Object.class, operacion, c.getCod(), c.getNom(), c.getTipCod(), c.getEstCod(),
        c.getFecIng(), c.getFecCes(), c.getFecUltProCer());
}
```
```sql
-- sp_cliente_mant, en PostgreSQL — AQUÍ vive la validación real
IF NOT EXISTS (SELECT 1 FROM gzz_tip_cli WHERE tip_cli_cod = p_tip_cod AND tip_cli_est_reg = 'A') THEN
  RAISE EXCEPTION 'El tipo de cliente % no existe o no está activo', p_tip_cod;
END IF;
INSERT INTO g1m_clientes (...) VALUES (...);
```

**La diferencia estructural**: en Django, `form.is_valid()` + `.save()`
concentra validación y persistencia en Python/el ORM. En GestProy, esas
tres capas Java (Controller → Service → Dao) son solo un **túnel** que
lleva los datos del formulario hasta PostgreSQL sin tocarlos; la validación
real (`RAISE EXCEPTION` si el tipo de cliente no existe) ocurre *dentro*
de la función SQL. Si vieras solo el código Java sin mirar `db/`, pensarías
que la aplicación "no valida nada" — sí valida, solo que en otro idioma y
en otro proceso (PostgreSQL, no la JVM).

---

## 4. Migraciones: el cambio de hábito más importante

En Django, cambiar un modelo es:
```bash
python manage.py makemigrations
python manage.py migrate
```
Django compara tu `models.py` contra el estado anterior y genera el SQL de
la migración automáticamente. Tienes un historial (`0001_initial.py`,
`0002_add_field.py`...) que puedes revertir con `migrate app 0001`.

**GestProy no tiene nada de esto.** Los archivos en `db/schema/*.sql` son
DDL puro y manual:

```sql
-- db/schema/02_maestras.sql
DROP TABLE IF EXISTS g1m_clientes CASCADE;
CREATE TABLE g1m_clientes ( ... );
```

Si quieres agregar una columna, editas ese archivo y vuelves a correr
`apply-all.ps1` — que hace `DROP TABLE ... CASCADE` y recrea todo desde
cero (por eso **borra los datos**, ver `03-DOCUMENTACION-GENERAL.md` §9.3).
No hay un mecanismo de "aplicar solo el cambio nuevo" como el de Django: en
este proyecto, cambiar el esquema en un entorno con datos reales
requeriría escribir un `ALTER TABLE` a mano y aplicarlo aparte — no está
automatizado, porque el foco del curso es diseñar el esquema correcto
desde el principio, no gestionar su evolución con el tiempo.

---

## 5. Autenticación: de `django-rest-framework-simplejwt` a JJWT + cookies

Esta es la comparación más directa de todo el documento porque el diseño
de GestProy **imita a propósito** el comportamiento de SimpleJWT en modo
cookie (que fue el pedido explícito al construir esta parte).

### Lo que ya conoces de Django

Con sesiones clásicas de Django (`django.contrib.sessions`), al hacer
login el servidor guarda una fila en la tabla `django_session` y le manda
al navegador una cookie `sessionid` (HttpOnly). En cada petición, Django
busca esa fila en la BD para saber quién sos — **el estado de la sesión
vive en el servidor**, la cookie solo es una llave para encontrarlo.

Con SimpleJWT (`djangorestframework-simplejwt`), el modelo cambia: no hay
tabla de sesiones. Al hacer login, el servidor firma un *access token* y
un *refresh token* — dos JWT autocontenidos — y (en su variante de
cookies) los manda como `Set-Cookie: access_token=...; HttpOnly` y
`Set-Cookie: refresh_token=...; HttpOnly`. Nadie en el servidor "recuerda"
la sesión: el propio token, con su firma, es la prueba de que sos quién
decís ser. El refresh token solo se invalida antes de tiempo si instalas
la app opcional de *blacklist* (una tabla que sí registra tokens revocados).

### Lo que hace GestProy

Exactamente ese segundo modelo, sin sesiones en BD:

| Pieza | SimpleJWT (Django) | GestProy |
|---|---|---|
| Librería de JWT | `djangorestframework-simplejwt` | `io.jsonwebtoken` (JJWT) — `security/JwtService.java` |
| Cookie de access token | `AUTH_COOKIE` (config `SIMPLE_JWT`) | `gp_access`, `HttpOnly`, `SameSite=Lax`, 15 min |
| Cookie de refresh token | `AUTH_COOKIE_REFRESH` | `gp_refresh`, `HttpOnly`, `SameSite=Lax`, 7 días |
| ¿Se guarda el refresh en BD? | No, salvo que actives `token_blacklist` | No, nunca — es una decisión deliberada (ver `03-DOCUMENTACION-GENERAL.md` §7.6) |
| Middleware que identifica al usuario | `JWTAuthentication` (DRF) | `JwtAuthFilter` (`OncePerRequestFilter`) |
| Quién resuelve "¿puede hacer esto?" | `permission_classes = [IsAuthenticated]` en la vista/ViewSet | `AutorizacionInterceptor`, aplicado globalmente en vez de por vista |
| Renovación del access token | Endpoint `POST /api/token/refresh/` que el frontend debe llamar a mano | **Automática y transparente**: `JwtAuthFilter` la hace en cualquier petición si detecta el access vencido — no existe un endpoint `/refresh` porque no hace falta un cliente JavaScript que lo invoque |
| Dónde se verifica la contraseña | `User.objects.get(username=...).check_password(...)` (hash con el hasher configurado, ej. PBKDF2/Argon2) | `fn_usuario_autenticar(login, pass)` en PL/pgSQL, con `crypt()` de pgcrypto (Blowfish) — la verificación ocurre **dentro de PostgreSQL**, no en Java |

**Por qué GestProy no necesita el endpoint `/token/refresh/` que sí usarías
en una API de Django**: en una SPA o app móvil consumiendo Django REST
Framework, el frontend (JavaScript) es quien decide cuándo pedir un token
nuevo, porque el frontend es el que arma las peticiones `fetch` a mano. En
GestProy no hay ningún JavaScript de por medio — el navegador manda las
cookies solo, en cada petición normal de formulario — así que quien puede
"decidir" renovar el token es el propio servidor, en el mismo filtro que
ya está mirando las cookies de todos modos.

**Analogía para `request.user`**: en Django, cualquier vista o template
puede leer `request.user` (o `{{ user }}` en el template) porque el
middleware de autenticación ya lo dejó ahí. En GestProy, el análogo es el
atributo de request `gestproy.admin` que deja `JwtAuthFilter`, expuesto a
las plantillas como la variable `admin` gracias a `GlobalModelAttributes`
(el equivalente a un *context processor* de Django). La diferencia es que
acá no es un objeto usuario completo, es solo `true`/`false` — porque solo
existe una cuenta.

---

## 6. Templates: Django Templates vs. Thymeleaf

Ambos son motores de plantillas *server-side*: el HTML final se arma en el
servidor, no en el navegador (a diferencia de React/Vue). La sintaxis
difiere, pero los conceptos calzan casi uno a uno:

| Django Templates | Thymeleaf | Ejemplo en GestProy |
|---|---|---|
| `{{ variable }}` | `th:text="${variable}"` | `<span th:text="${cliente.nom}">` |
| `{% for x in lista %}` | `th:each="x : ${lista}"` | `<tr th:each="c : ${clientes}">` |
| `{% if condicion %}` | `th:if="${condicion}"` | `<a th:if="${admin}" ...>Adicionar</a>` |
| `{% extends "base.html" %}` | `th:replace="~{fragmento :: nombre}"` | `<nav th:replace="~{fragments/nav :: nav}">` |
| `{% include "nav.html" %}` | Igual que arriba: Thymeleaf no distingue "extends" de "include", todo es reemplazo de fragmentos | Cada página de GestProy arma su propio `<head>`/`<nav>` a partir de fragmentos, en vez de heredar de una plantilla base única |
| `{% url 'nombre_ruta' arg %}` | `@{/ruta/{arg}(arg=${valor})}` | `th:href="@{/clientes/{c}/editar(c=${c.cod})}"` |
| `{% csrf_token %}` | *(no existe — ver hueco de seguridad)* | GestProy no tiene protección CSRF explícita, ver `03-DOCUMENTACION-GENERAL.md` §11.2 |
| Filtros (`{{ valor\|date:"Y-m-d" }}`) | Utilidades `#numbers`, `#dates` de Thymeleaf | `th:text="${#numbers.formatDecimal(p.cosHor, 1, 2)}"` |

Una diferencia real (no solo de sintaxis): GestProy **no** tiene una
plantilla base única de la que todas hereden (`base.html` con
`{% block content %}`). Cada archivo `.html` repite su propio
`<head>`/`<nav>` vía fragmentos — el `PLAN.md` original proponía un
`layout/base.html`, pero la implementación final no lo adoptó. Es un
patrón más parecido a incluir un *partial* en cada página que a la
herencia de plantillas de Django.

---

## 7. Inyección de dependencias: el contenedor de Spring vs. los imports de Django

En Django, si una vista necesita usar otra función o clase, simplemente la
**importas**:
```python
from myapp.services import crear_cliente
```
No hay un paso de "registro" — Python resuelve el import y ya.

En Spring Boot, las clases se anotan con un estereotipo
(`@Service`, `@Repository`, `@Controller`) y Spring las **construye por
ti** e inyecta sus dependencias automáticamente por tipo, vía el
constructor:

```java
@Service
public class ClienteService {
    private final ClienteDao dao;                 // Spring inyecta esto
    public ClienteService(ClienteDao dao) {        // con solo declarar este constructor
        this.dao = dao;
    }
}
```

Nunca escribes `new ClienteService(new ClienteDao(...))` en ningún lado —
Spring arma todo el grafo de objetos al arrancar (parecido, si te sirve la
comparación, a como Django arma su `INSTALLED_APPS` y el *app registry* al
arrancar, aunque Django no hace inyección de dependencias real: los
objetos Django se importan directamente, no se "piden" a un contenedor).
Esto es la razón por la que casi ninguna clase de GestProy usa la palabra
`new` para crear sus colaboradores — todo llega ya armado por el
constructor.

---

## 8. Dónde mirar según lo que quieras encontrar

| Si en Django buscarías... | En GestProy busca en... |
|---|---|
| `models.py` | `db/schema/*.sql` (estructura) + `model/*.java` (solo el molde de datos, sin comportamiento) |
| Lógica de negocio en el modelo o en un signal | Funciones PL/pgSQL en `db/functions/*.sql` |
| `views.py` | `web/*.java` (`@Controller`) |
| `urls.py` | Anotaciones `@RequestMapping` dentro de cada controller (no hay archivo centralizado) |
| `forms.py` | No existe una clase `Form` separada: el propio modelo (`Cliente`, `Proyecto`...) se rellena directo vía `@ModelAttribute` |
| `admin.py` | No existe (no hay admin autogenerado) |
| `settings.py` | `application.properties` (config general) + `db.properties` (secretos, gitignored) |
| `migrations/` | `db/schema/*.sql`, aplicados manualmente y sin historial versionado por Django |
| Tests (`tests.py`, `pytest`) | `db/tests/smoke_tests.sql` (tests de la lógica SQL, no de Java) |
| `manage.py` | `pom.xml` + Maven (`mvn spring-boot:run`, `mvn compile`, `mvn test`) |

---

## 9. Glosario mínimo Java/Spring para alguien de Django

| Término | Qué es, en criollo |
|---|---|
| `@Component`/`@Service`/`@Repository`/`@Controller` | Anotaciones que le dicen a Spring "creá una instancia de esta clase y gestionala vos" (sin esto, la clase es invisible para Spring) |
| Bean | Cualquier objeto que Spring creó y gestiona (más o menos como cualquier objeto que vive en el *app registry* de Django, pero con inyección real) |
| `@Autowired` / inyección por constructor | Cómo un bean recibe a otro bean que necesita, sin que nadie escriba `new` |
| POJO | *Plain Old Java Object*: una clase Java simple, sin heredar de ningún framework — el equivalente a una clase Python común sin decoradores especiales |
| `record` (Java 16+) | Una clase inmutable de solo datos, declarada en una línea — el pariente más cercano en Python es un `NamedTuple` o un `dataclass(frozen=True)` |
| `Optional`/`null` | Java no tiene `None` con el mismo tratamiento en todos lados: muchos métodos devuelven `null` directamente (hay que revisarlo a mano), a diferencia de Python donde `None` es más uniforme |
| `RuntimeException` | Una excepción que no es obligatorio declarar ni capturar (a diferencia de las *checked exceptions* de Java, que si no atrapás, no compila) — se comporta como cualquier excepción de Python |
| `.jar` | El artefacto empaquetado y ejecutable, análogo a una imagen Docker autocontenida o a un *wheel* instalable, pero para la JVM |
| Tomcat embebido | El servidor HTTP que corre *dentro* del proceso Java (como el `runserver` de desarrollo de Django, pero pensado para producción real, no solo para desarrollo) |
| `application.properties` | El único archivo de config "base" de Spring Boot — Django no tiene un equivalente 1:1 porque `settings.py` es código Python ejecutable, mientras que esto es solo clave=valor |

---

## 10. Lo que sí es prácticamente igual

Para que no todo suene a lista de diferencias — esto se traslada casi sin
cambios de un mundo al otro:

- **El patrón Post-Redirect-Get**: en Django también rediriges tras un
  `POST` exitoso para evitar reenvíos al recargar. GestProy hace lo mismo.
- **Mensajes flash**: `messages.success(request, "...")` de Django y
  `RedirectAttributes.addFlashAttribute("exito", "...")` de GestProy
  resuelven el mismo problema de la misma forma.
- **Cookies HttpOnly para sesión**: el modelo mental es idéntico al de
  Django (`SESSION_COOKIE_HTTPONLY = True` es el default hace años); acá
  simplemente no hay una fila en una tabla de sesiones detrás de la cookie.
- **Server-rendered HTML**: ambos frameworks asumen que el HTML se genera
  en el servidor. Si sabés pensar en templates Django, ya sabés pensar en
  Thymeleaf — solo cambia la sintaxis de las expresiones.
- **La filosofía de "no repitas código"**: `ReferencialTabla` (un enum que
  parametriza 9 tablas con un solo controller/DAO) es la misma idea detrás
  de usar una sola `ListView` genérica de Django para varios modelos
  parecidos, parametrizada por `model = ...` en vez de un enum.
