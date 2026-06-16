**UNIVERSIDAD NACIONAL DE SAN AGUSTIN FACULTAD DE INGENIERÍA DE PRODUCCIÓN Y SERVICIOS ESCUELA PROFESIONAL DE INGENIERÍA DE SISTEMA** 

**Formato:** Guía de Práctica de Laboratorio / Talleres / Centros de Simulación 

~~en~~ **Aprobación:  2022/03/01 Código: GUIA-PRLD-001 Página:** 1 

# **GUÍA DE LABORATORIO** GUIA DE LABORATORIO 

# **(formato docente)** 

## **INFORMACIÓN BÁSICA** 

|**ASIGNATURA:**|Base de Datos|||||
|---|---|---|---|---|---|
|**TÍTULO DE LA**<br>**PRÁCTICA:**|SQL MySQL Carga de Datos a la Base de Datos|||||
|**NÚMERO DE**<br>**PRÁCTICA:**|07|**AÑO LECTIVO:**|2026 A|**NRO.**<br>**SEMESTRE:**|V|
|**TIPO DE**<br>**PRÁCTICA:**|**INDIVIDUAL**<br>**GRUPAL**|**MÍNIMO DE**<br>**ESTUDIANTES**|_2_|**MÁXIMO DE**<br>**ESTUDIANTES**|_3_|
|**FECHA INICIO:**|15 06 2026|**FECHA FIN:**|26 06 2026|**DURACIÓN:**|4 horas|



## **RECURSOS A UTILIZAR:** 

_Computador; Sistema de Gestión de Base de Datos MySQL / Postgresql, Lenguaje de Programación JAVA/C++._ **DOCENTE(s):** Dr. Ing. César Baluarte Araya 

Mg. Ing. Edith Giovanna Cano Mamani 

Mg. Robert Arizaca Mamani 

- Mg. José Delgado Bastidas 

## **OBJETIVOS/TEMAS Y COMPETENCIAS** 

## **OBJETIVOS:** 

- Elaborar los programas de carga de datos a la base de datos MySQL/PostgreSQL 

- Establecer la conexión a la base de datos MySQL/PostgreSQL para así manipular los datos con los programas de aplicación generados con el lenguaje de programación JAVA. 

- Investigar para complementar el conocimiento sobre el tema de la sesión. 

## **TEMAS:** 

- Conexión a la base de datos 

- Programas de aplicación para carga de datos a la base de datos 

- Herramienta de gestión de base de datos – MySQL/PostgreSQL 

- Herramienta de programación - JAVA. 

**COMPETENCIAS** C.e _Identifica de forma reflexiva y responsable, necesidades a ser resueltas usando tecnologías de información y/o desarrollo de software en los ámbitos local, nacional o internacional, utilizando técnicas, herramientas, metodologías, estándares y principios de la ingeniería._ 

C.m _Construye responsablemente soluciones siguiendo un proceso adecuado llevando a cabo las pruebas ajustada a los recursos disponibles del cliente._ C.p _Aplica de forma flexible técnicas, métodos, principios, normas, estándares y herramientas de ingeniería necesarias para la construcción de software e implementación de sistemas de información._ 

**UNIVERSIDAD NACIONAL DE SAN AGUSTIN FACULTAD DE INGENIERÍA DE PRODUCCIÓN Y SERVICIOS ESCUELA PROFESIONAL DE INGENIERÍA DE SISTEMA** ABET **Formato:** Guía de Práctica de Laboratorio / Talleres / Centros de Simulación **Aprobación:  2022/03/01 Código: GUIA-PRLD-001 Página:** 2 

## **CONTENIDO DE LA GUÍA** 

**==> picture [126 x 9] intentionally omitted <==**

**----- Start of picture text -----**<br>
I. MARCO CONCEPTUAL<br>**----- End of picture text -----**<br>


## **Conexión a la base de datos** 

Para trabajar las tablas de la base de datos del tema asignado, se debe realizar la conexión, y con el primer programa a desarrollar efectuar su operación o funcionamiento. 

## **Estándares de Desarrollo de software** 

Para el desarrollo de los programas de aplicación utilizar el estándar a aplicar para la nomenclatura de los elementos que intervendrán en su elaboración. 

Para el desarrollo de los programas se realizarán con el lenguaje de programación JAVA u otro. 

## **Especificaciones de programación** 

Para el desarrollo de los programas se tomará como pauta las siguientes especificaciones de programación de un programa que efectúe el mantenimiento a una tabla referencial (Ejemplos de tablas referenciales tenemos a: Genero, Países, Cargo, Estado Civil, Tipo de xxxxx, etc.) de acuerdo al tema que está desarrollando. 

Se muestra el diseño aproximado de mantenimiento de una tabla (CARGO) de un trabajador en una organización, por ejemplo, en base al cual daremos las especificaciones. 

- El nombre del programa es para este caso  CARGO  si utilizáramos el estándar sería: **R13001 - CARGO** 

**UNIVERSIDAD NACIONAL DE SAN AGUSTIN FACULTAD DE INGENIERÍA DE PRODUCCIÓN Y SERVICIOS ESCUELA PROFESIONAL DE INGENIERÍA DE SISTEMA Formato:** Guía de Práctica de Laboratorio / Talleres / Centros de Simulación **Aprobación:  2022/03/01 Código: GUIA-PRLD-001 Página:** 3 ~~a~~ 

Del sistema R = Recursos Humanos Del subsistema 1 = Maestro de Personal Tipo 3 = Mantenimiento de archivos Secuencia = 001) 

• Se tiene la parte Registro de Cargo que es el ambiente de trabajo para efectuar las funciones que hace el programa 

• Se tiene la parte de la Grilla que mostrará las instancias (ocurrencias, instancias, filas, tuplas o registros) de la tabla que se trabaja, teniendo que los datos son: Datos Nemotécnico Código del cargo CarCod Descripción del cargo CarDes Estado de Registro CarEstReg de la tabla Cargo (se coloca por defecto A = Activo; existiendo para él los otros valores de I = Inactivo ***** = Eliminado) 

Se tiene los comandos con que trabajará el programa, los que se describen en la operatividad. 

- La operatividad para este programa por fines de seguridad y de orden a desarrollar serán como se describe: 

- **Adicionar nuevos registros a la tabla de base de datos** 

   - **Comando Adicionar** ; se seleccionará el comando Adicionar y se blanqueará las cajas de texto del área de Registro, procediendo a ingresar los datos de código y de descripción (el dato estado de registro se carga en su campo por defecto la letra **A** = Activo) y no podrá ser modificado por un usuario, estando protegido de ello. 

Se coloca el valor de “1” en el flag o bandera de actualizar (nombre del flag CarFlaAct); que nos indicará que se actualizará un registro en la base de datos. 

Se selecciona el comando Actualizar (verificando que el Flag CarFlaAct tenga el valor de “1” para grabar en la tabla de la base de datos) si es así se graba en la BD y se carga el registro adicionado (código, descripción y estado del registro) en la grilla. 

Si el valor del Flag CarFlaAct es “0” se emite el mensaje (“No se ha seleccionado un comando para actualizar un registro de la BD” y que el mensaje tenga la opción de solo CANCELAR el mensaje). 

Si no se desea Actualizar se selecciona comando Cancelar, se borra los datos del área de Registro y se inactiva el adicionar. Se coloca el flag o bandera de actualizar en valor de “0” no realizará la función de actualizar. 

- **Modificar registros a la tabla de la base de datos** 

`o` **Comando Modificar** ; se selecciona con un **click** el registro de la grilla que se desea modificar, se seleccionará el comando Modificar y se carga los datos del registro seleccionado que se desea modificar a las cajas de texto del área de Registro. (sólo se puede modificar la descripción, protegiendo el dato código y estado de registro). 

Se coloca el valor de “1” en el flag o bandera de actualizar (nombre del flag CarFlaAct); que nos indicará que se actualizará un registro en la base de datos. 

**UNIVERSIDAD NACIONAL DE SAN AGUSTIN FACULTAD DE INGENIERÍA DE PRODUCCIÓN Y SERVICIOS ESCUELA PROFESIONAL DE INGENIERÍA DE SISTEMA** 

**Formato:** Guía de Práctica de Laboratorio / Talleres / Centros de Simulación **Aprobación:  2022/03/01 Código: GUIA-PRLD-001** ~~a~~ 

**Página:** 4 

Se selecciona el comando Actualizar (verificando que el Flag CarFlaAct tenga el valor de “1” para grabar en la tabla de la base de datos) si es así se graba en la BD y se carga el registro modificado (código, descripción y estado del registro) en la grilla. Si el valor del Flag CarFlaAct es “0” se emite el mensaje (“No se ha seleccionado un comando para actualizar un registro de la BD” y que el mensaje tenga la opción de solo CANCELAR el mensaje). 

Si no se desea Actualizar se selecciona comando Cancelar, se borra los datos del área de Registro y se inactiva el modificar. Se coloca el flag o bandera de actualizar en valor de “0” no realizará la función de actualizar. 

## • **Eliminar registros a la tabla de la base de datos** 

`o` **Comando Eliminar** ; se selecciona con un click el registro de la grilla que se desea eliminar, se seleccionará el comando Eliminar, y se carga los datos del registro seleccionado que se desea eliminar a las cajas de texto del área de Registro. (No se puede modificar ningún dato; protegiendo el dato código, descripción y estado de registro). 

Se coloca *** = Eliminado** en el dato estado de registro (No se elimina físicamente ningún registro de la tabla de la base de datos, porque así marcando con ***** la eliminación es lógica). 

Se coloca el valor de “1” en el flag o bandera de actualizar (nombre del flag CarFlaAct); que nos indicará que se actualizará un registro en la base de datos. 

Se selecciona el comando Actualizar (verificando que el Flag CarFlaAct tenga el valor de “1” para grabar en la tabla de la base de datos) si es así se graba en la BD y se carga el registro eliminado (código, descripción y estado del registro) en la grilla. 

Si el valor del Flag CarFlaAct es “0” se emite el mensaje (“No se ha seleccionado un comando para actualizar un registro de la BD” y que el mensaje tenga la opción de solo CANCELAR el mensaje). 

Si no se desea Actualizar se selecciona comando Cancelar, se borra los datos del área de Registro y se inactiva el eliminar. Se coloca el flag o bandera de actualizar en valor de “0” no realizará la función de actualizar. 

## • **Inactivar registros en la tabla de la base de datos** 

- **Comando Inactivar** ; se selecciona con un click el registro de la grilla que se desea inactivar; se seleccionará el comando Inactivar, y se carga los datos del registro seleccionado que se desea inactivar a las cajas de texto del área de Registro. (No se puede modificar ningún dato; protegiendo el dato código, descripción y estado de registro). 

Se coloca **I = Inactivo** en el dato estado de registro (esto sirve para no seguir usando un determinado registro de la tabla por x razones, puede ser por algún tiempo determinado u otra causa). 

Se coloca el valor de “1” en el flag o bandera de actualizar (nombre del flag CarFlaAct); que nos indicará que se actualizará un registro en la base de datos. 

**UNIVERSIDAD NACIONAL DE SAN AGUSTIN FACULTAD DE INGENIERÍA DE PRODUCCIÓN Y SERVICIOS ESCUELA PROFESIONAL DE INGENIERÍA DE SISTEMA** 

**Formato:** Guía de Práctica de Laboratorio / Talleres / Centros de Simulación **Aprobación:  2022/03/01 Código: GUIA-PRLD-001** ~~a~~ 

**Página:** 5 

Se selecciona el comando Actualizar (verificando que el Flag CarFlaAct tenga el valor de “1” para grabar en la tabla de la base de datos) si es así se graba en la BD y se carga el registro inactivado (código, descripción y estado del registro) en la grilla. Si el valor del Flag CarFlaAct es “0” se emite el mensaje (“No se ha seleccionado un comando para actualizar un registro de la BD” y que el mensaje tenga la opción de solo CANCELAR el mensaje). 

Si no se desea Actualizar se selecciona comando Cancelar, se borra los datos del área de Registro y se inactiva el inactivar. Se coloca el flag o bandera de actualizar en valor de “0” no realizará la función de actualizar. 

## • **Reactivar registros en la tabla de la base de datos** 

`o` **Comando Reactivar** ; se selecciona con un click el registro de la grilla que se desea reactivar; se seleccionará el comando Reactivar, y se carga los datos del registro seleccionado que se desea reactivar a las cajas de texto del área de Registro. (No se puede modificar ningún dato; protegiendo el dato código, descripción y estado de registro). 

Se coloca **A = Activo** en el dato estado de registro (esto sirve para tener el registro otra vez activo, ya que se inactivó o que se equivocaron al eliminarlo) 

Se coloca el valor de “1” en el flag o bandera de actualizar (nombre del flag CarFlaAct); que nos indicará que se actualizará un registro en la base de datos. 

Se selecciona el comando Actualizar (verificando que el Flag CarFlaAct tenga el valor de “1” para grabar en la tabla de la base de datos) si es así se graba en la BD y se carga el registro reactivado (código, descripción y estado del registro) en la grilla. 

Si el valor del Flag CarFlaAct es “0” se emite el mensaje (“No se ha seleccionado un comando para actualizar un registro de la BD” y que el mensaje tenga la opción de solo CANCELAR el mensaje). 

Si no se desea Actualizar se selecciona comando Cancelar, se borra los datos del área de Registro y se inactiva el reactivar. Se coloca el flag o bandera de actualizar en valor de “0” no realizará la función de actualizar. 

## • **Actualizar registros en la tabla de la base de datos** 

   - **Comando Actualizar** ; se detalló su funcionabilidad en los comandos anteriores descritos. Si se selecciona el comando Actualizar debe estar el flag o bandera de actualizar con un valor de “1” (se colocó previamente en “1” por la función de: adicionar, modificar, eliminar, inactivar o reactivar) para proceder a realizar su función de: 

      - Grabar en la tabla de la base de datos. 

      - Cargar el registro actualizado (código, descripción y estado del registro) en la grilla. 

      - Colocar el flag o bandera de actualizar en valor de “0” cuando no realizará la función de actualizar el registro (se utilizó el Comando Cancelar) o cuando se actualizó adecuadamente el registro. 

- **Cancelar una función que se está trabajando en la tabla de la base de datos** 

   - **Comando Cancelar** , se utiliza para cancelar la función que se esté trabajando, se borra los datos del área de Registro y se inactiva la función que se estuvo trabajando. 

**UNIVERSIDAD NACIONAL DE SAN AGUSTIN FACULTAD DE INGENIERÍA DE PRODUCCIÓN Y SERVICIOS ESCUELA PROFESIONAL DE INGENIERÍA DE SISTEMA** 

**Formato:** Guía de Práctica de Laboratorio / Talleres / Centros de Simulación 

~~rs~~ **Aprobación:  2022/03/01 Código: GUIA-PRLD-001 Página:** 6 

## • **Salir para terminar la ejecución del programa** 

`o` **Comando Salir** , se utiliza para cancelar cualquier función de los comandos que se esté trabajando, se borra los datos del área de Registro y se termina la ejecución del programa. 

A continuación, se dan figuras que muestran cómo es la funcionabilidad. Por ejemplo: el Comando Adicionar; se ingresa el código y la descrpción 

Al dar Actualizar se actualiza la BD, se carga la grilla y se blanquean las cajas de texto, las cuales quedan como se muestra 

**UNIVERSIDAD NACIONAL DE SAN AGUSTIN FACULTAD DE INGENIERÍA DE PRODUCCIÓN Y SERVICIOS ESCUELA PROFESIONAL DE INGENIERÍA DE SISTEMA** 

**Formato:** Guía de Práctica de Laboratorio / Talleres / Centros de Simulación **Aprobación:  2022/03/01 Código: GUIA-PRLD-001 Página:** 7 

Se siguen adicionando registros. 

## **UNIVERSIDAD NACIONAL DE SAN AGUSTIN FACULTAD DE INGENIERÍA DE PRODUCCIÓN Y SERVICIOS ESCUELA PROFESIONAL DE INGENIERÍA DE SISTEMA** 

**Formato:** Guía de Práctica de Laboratorio / Talleres / Centros de Simulación 

**Aprobación:  2022/03/01** ~~a~~ 

**Código: GUIA-PRLD-001** 

**Página:** 8 

## **II. EJERCICIO RESUELTO** 

## **La práctica tiene una duración de 04 horas** 

01. Va a trabajar con su computador. 

02. Debió previamente tener instalado el MySQL / PostgreSQL y el software de trabajo para la plataforma de software libre 

03. Debió haber creado la base de datos de su tema o caso estudio académico asignado; ya se hizo en las sesiones anteriores a la presente 

04. Efectuar la conexión de la base de datos con Java u otro lenguaje de programación y probar su respuesta. 

## **Esta sesión tiene 3 entregables:** 

- 1 Mantenimiento de **una Tabla Referencial** de la base de datos de su tema o caso estudio académico asignado, y cargado de los datos de sus registros (Ejemplo: tabla IDIOMAS, se carga Español, Inglés, Francés, Alemán, etc) 

- 2 Mantenimiento de **las demás Tablas Referenciales** de la base de datos de su tema o caso estudio académico asignado, y cargado de los datos de sus registros 

- 3 Mantenimiento de **las Otras Tablas Maestras, Transacciones, Control, etc** . de la base de datos de su tema o caso estudio académico asignado, y cargado de los datos de sus registros. 

## **Observación.-** 

Para esta sesión **sólo en el primer Informe de Entregable se desarrollará el Marco Teórico** . En esta sesión **se presentará tanto el Primer y Segundo Informe de Entregable en la misma fecha** . 

La carga de los datos a las tablas de la base de datos, **SOLO SE REALIZA POR EL PROGRAMA QUE SE DESARROLLA PARA CADA UNA DE ELLAS** . 

En la presentación del subgrupo en el Laboratorio **se ejecutará el o los programas que el docente de Laboratorio indique se ejecuten** según sea en **Informe de Entregable** . 

## **PRIMER INFORME DE ENTREGABLE** 

05. Marco Teórico: 

El marco teórico es la recopilación y descripción detallada de los elementos teóricos como los Conceptos, Estándares, Metodologías, Métodos, Técnicas, Trabajos, que se utilizaron directamente en el desarrollo de la investigación/trabajo/proyecto, contemplando la notación APA, IEEE u otra según corresponda y como se referencia en el texto. 

- **Conceptos Previos** 

Elaborar la relación de todos los conceptos previos con una descripción resumida de cada uno de ellos en máximo 4 líneas (son los mostrados en el marco teórico de la presente guía de práctica). 

- **Conceptos Nuevos** 

   - Del problema 

   - Técnicos 

- **Estándares** 

**UNIVERSIDAD NACIONAL DE SAN AGUSTIN FACULTAD DE INGENIERÍA DE PRODUCCIÓN Y SERVICIOS ESCUELA PROFESIONAL DE INGENIERÍA DE SISTEMA Formato:** Guía de Práctica de Laboratorio / Talleres / Centros de Simulación **Aprobación:  2022/03/01 Código: GUIA-PRLD-001** ~~a~~ 

**Página:** 9 

- **Metodologías** 

- • **Métodos** 

- **Técnicas** 

- **Trabajos** 

- Se redacta las investigaciones efectuadas por otros: • Tesis 

   - Artículos o papers 

   - Casos de Éxito logrados en otras organizaciones 

   - Se considera los siguientes puntos: 

   - Titulo 

   - Autores 

   - Año 

   - Problema 

   - Resultados o Conclusiones (enumerados con viñetas). 

- **Herramientas** 

Deberá realizar para el Marco Teórico: 

Busque, indague, revise situaciones similares, revisa literatura relacionada, recopile, organice, interprete datos; de los puntos señalados anteriormente, tratándolos de forma muy puntual y concreta. 

Esta actividad va en el **Informe del Entregable** (s) de la sesión que es(son) evaluado(s). 

Estos puntos del Marco Teórico desarrollados le servirán para ser incluidos en el **Informe de la Investigación Formativa** , que se entrega al final del semestre según plantilla diseñada para tal, el cual es evaluado. 

06. Va a trabajar con su computador. 

07. Elaborar el programa de mantenimiento de **una Tabla Referencial** de la base de datos de su tema o caso estudio académico asignado, siguiendo las especificaciones anteriormente mostradas (del punto: Especificaciones de Programación); lo único que cambia son los nombres de los elementos datos de la tabla referencial que usted va a trabajar. 

08. Elaborar a la par el informe con los **Print Screen** o impresión de las pantallas que son trabajadas de acuerdo a la funcionabilidad 

Se siguen las siguientes pautas para los Print Screen: 

- Pantalla de área de registro sin datos y grilla sin datos 

- Pantalla de área de registro con datos y grilla sin datos 

- Pantalla de área de registro sin datos y grilla con datos 

- Pantalla de la tabla de la base de datos con registro (grabado, se debe usar la opción de visualizar datos de la tabla del SGBD) 

- Pantalla de área de registro con datos (2do. Registro)  y grilla (muestra el 1er. registro) 

- Pantalla de área de registro sin datos y grilla (1er. y 2do. registro cargado) 

- Pantalla de la tabla de la base de datos con 2 registros (grabados, se debe usar la opción de visualizar datos de la tabla del SGBD) 

- • Ingresar los demás registros de la tabla (completar todos) 

**UNIVERSIDAD NACIONAL DE SAN AGUSTIN FACULTAD DE INGENIERÍA DE PRODUCCIÓN Y SERVICIOS ESCUELA PROFESIONAL DE INGENIERÍA DE SISTEMA** 

**Formato:** Guía de Práctica de Laboratorio / Talleres / Centros de Simulación **Aprobación:  2022/03/01 Código: GUIA-PRLD-001** ~~a~~ 

**Página:** 10 

- Pantalla de área de registro sin datos y grilla (con todos los registros cargados) 

- Pantalla de la tabla de la base de datos con todos los registros (grabados, se debe usar la opción de visualizar datos de la tabla del SGBD) 

Para la función de Modificar un registro se siguen las siguientes pautas para los print screen: 

- Pantalla de área de registro sin datos y grilla con muchos registros de datos de la tabla 

- Pantalla que muestre el registro de la grilla que se selecciona para modificar datos 

- Pantalla que muestre el registro que se seleccionó, mostrando los datos del mismo en las cajas de texto del área de registro 

- Pantalla que muestre el dato que se modificó en el área de registro antes de dar Actualizar 

- Pantalla de área de registro sin datos y grilla con datos que permita visualizar el registro actualizado 

- Pantalla de la tabla de la base de datos que muestre el dato del registro modificado. 

Se utiliza el mismo criterio base (que se siguió para la función modificar) para las otras funciones de: eliminar, inactivar, reactivar, cancelar. 

09. Crear el informe consistente en reflejar el resultado del desarrollo del programa de mantenimiento de la Tabla Referencial y la muestra de las funciones del programa, adicionar, modificar, eliminar, inactivar, reactivar, actualizar, con los **Print Screen** de lo trabajado en ellas. 

10. Incluir los **print screens** de lo realizado que demuestre el desarrollo de las actividades de la práctica (en una página caben 2 print screen y unas 2 a 3 líneas de explicación). 

Estos print screens van en el punto correspondiente del **Informe de Entregable** del tema tratado. 

11. Elaborar el **Informe del Entregable (8)** especificando también en su contenido además lo siguiente: 

   - a. De la experiencia del diseño del software para la carga de datos, emita opinión de cuan importante es aplicar principios éticos en el generar las especificaciones de programación completas para la obtención del producto final por el diseño de la base de datos relacional. 

   - b. De la experiencia del diseño del software para la carga de datos, emita opinión sobre la responsabilidad de comprender la importancia de diseñar las especificaciones de programación completas y claras del software para la carga de datos en la BD en base al diseño del modelo relacional. 

## **SEGUNDO INFORME DE ENTREGABLE** 

12. Elaborar los programas de mantenimiento de **las Otras Tablas Referenciales** del tema o caso estudio académico asignado. Se siguen las especificaciones del PRIMER ENTREGABLE, ya que lo único que cambia son: el código y nombre del programa, los nombres de los elementos datos de la(s) tabla(s) (código, descripción, estado de registro). 

13. Elaborar a la par el informe con los Print Screen de las tablas involucradas 

Se siguen las siguientes pautas para los Print Screen: 

• Pantalla de área de registro sin datos y grilla con datos de (1er. y 2do. registro cargado) 

**UNIVERSIDAD NACIONAL DE SAN AGUSTIN FACULTAD DE INGENIERÍA DE PRODUCCIÓN Y SERVICIOS ESCUELA PROFESIONAL DE INGENIERÍA DE SISTEMA** 

**Formato:** Guía de Práctica de Laboratorio / Talleres / Centros de Simulación **Aprobación:  2022/03/01 Código: GUIA-PRLD-001** ~~a~~ 

**Página:** 11 

- Pantalla de la tabla de la base de datos con 2 registros (grabados, se debe usar la opción de visualizar datos de la tabla del SGBD) 

- Ingresar los demás registros de la tabla (completar todos) 

- Pantalla de área de registro sin datos y grilla (con todos los registros cargados) 

- Pantalla de la tabla de la base de datos con todos los registros (grabados, se debe usar la opción de visualizar datos de la tabla del SGBD) 

Se utiliza las mismas pautas base descritas en líneas anteriores para mostrar el resultado de las Otras Tablas Referenciales (todas). 

14. Crear el informe consistente en reflejar el resultado del desarrollo de los programas de mantenimiento de las Otras Tablas Referenciales, con los Print Screen de lo trabajado en ellas donde se muestran los registros cargados en la pantalla del programa. 

15. Incluir los **print screens** de lo realizado que demuestre el desarrollo de las actividades de la práctica (en una página caben 2 print screen y unas 2 a 3 líneas de explicación). 

Estos print screens van en el punto correspondiente del **Informe de Entregable** del tema tratado. 

16. Elaborar el **Informe del Entregable (9)** especificando también en su contenido además lo siguiente: 

   - a. De la experiencia del diseño de la base de datos y del software para la carga de datos, emita opinión de cuan importante es aplicar principios éticos en el diseño de la base de datos para la obtención de los productos finales de software para el tratamiento de las Tablas Referenciales por el diseño de la base de datos relacional. 

   - b. De la experiencia del diseño de la base de datos y del software para la carga de datos, emita opinión sobre la responsabilidad de comprender la importancia del diseño físico de la base de datos para obtener productos finales de software en el tratamiento de la Tablas Referenciales y su implicancia en el diseño de la base de datos relacional. 

## **TERCER INFORME DE ENTREGABLE** 

17. Elaborar los programas de mantenimiento de **las Otras Tablas Fundamentales (Maestras, Transacciones, Control, etc.)** y dependientes 

   - Ejemplo: Clientes, Clientes Contactos, Clientes Crédito Otro ejemplo Activos Fijos y Especificaciones del Activo Fijo Otro ejemplo Vale de Salida Cabecera, Vale de Salida Detalle, Vale de Salida Observaciones. 

   - **…** 

18. Elaborar a la par el informe con los Print Screen de las tablas involucradas 

Se siguen las siguientes pautas para los Print Screen: 

- Pantalla de área de registro con datos (1er. Registro)  y si contiene grilla (esta se muestra sin datos) 

- Pantalla de área de registro con datos (2do. Registro)  y si contiene grilla (esta muestra el 1er. registro) 

**UNIVERSIDAD NACIONAL DE SAN AGUSTIN FACULTAD DE INGENIERÍA DE PRODUCCIÓN Y SERVICIOS ESCUELA PROFESIONAL DE INGENIERÍA DE SISTEMA** 

**Formato:** Guía de Práctica de Laboratorio / Talleres / Centros de Simulación 

**Aprobación:  2022/03/01 Código: GUIA-PRLD-001** ~~a~~ 

**Página:** 12 

- Pantalla de la tabla de la base de datos con 2 registros (grabados, se debe usar la opción de visualizar datos de la tabla del SGBD) 

- Ingresar los demás registros de la tabla ( **por lo menos 20 o más registros para Maestros** y **30 o más registros para transacciones o movimientos; variando los datos de registro a registro** ), esto es importante para ser utilizado en las siguientes sesiones de Laboratorio. 

- Pantalla de área de registro sin datos y si contiene grilla (esta muestra todos los registros cargados 

- Pantalla de la tabla de la base de datos con todos los registros (grabados, se debe usar la opción de visualizar datos de la tabla del SGBD) 

Se utiliza las mismas pautas base descritas en líneas anteriores para mostrar el resultado de las Otras Tablas Fundamentales (todas). 

**IMPORTANTE:** Los datos cargados serán utilizados en las **siguientes sesiones de laboratorio** ; si adicionan más registros a las Tablas Fundamentales verán mejor los resultados que obtendrán y así podrán validar la BD y por ende la Calificación será la más adecuada. 

19. Crear el informe consistente en reflejar el resultado del desarrollo de los programas de mantenimiento de las Otras Tablas Fundamentales, con los Print Screen de lo trabajado en ellas donde se muestran los registros cargados en la pantalla del programa. 

20. Incluir los **print screens** de lo realizado que demuestre el desarrollo de las actividades de la práctica (en una página caben 2 print screen y unas 2 a 3 líneas de explicación). 

Estos print screens van en el punto correspondiente del **Informe de Entregable** del tema tratado. 

21. Elaborar el **Informe del Entregable (10)** de acuerdo a la Estructura del Informe de Entregable establecido para este fin; especificando también en su contenido además lo siguiente: 

   - a. De la experiencia del desarrollo de software para la carga de datos, emita opinión de cuan importante es aplicar principios éticos en el desarrollo de software que valide el diseño de la base de datos. 

   - b. De la experiencia del desarrollo de software para la carga de datos, emita opinión sobre la responsabilidad de comprender la importancia del adecuado desarrollo del software que permita validar el modelamiento de la realidad del problema a través del diseño físico de la base de datos. 

## **III. EJERCICIOS/PROBLEMAS PROPUESTOS** 

- En base a la creación del programa base desarrolle un segundo programa de Otra Tabla Fundamental relacionada con la Tabla Referencial desarrollada en la sesión anterior. 

## **IV. CUESTIONARIO** 

1. ¿Qué otras funcionalidades se pueden incluir al programa de mantenimiento de la tabla trabajada? 

2. ¿Sería conveniente introducir la opción de búsqueda en la grilla? 

**UNIVERSIDAD NACIONAL DE SAN AGUSTIN FACULTAD DE INGENIERÍA DE PRODUCCIÓN Y SERVICIOS ESCUELA PROFESIONAL DE INGENIERÍA DE SISTEMA** 

**Formato:** Guía de Práctica de Laboratorio / Talleres / Centros de Simulación **Aprobación:  2022/03/01 Código: GUIA-PRLD-001** ~~a~~ 

**Página:** 13 

3. ¿A partir de lo desarrollado como programa, que se tendría que hacer para que el mismo sea más operativo? 

## **V. REFERENCIAS Y BIBLIOGRAFÍA RECOMENDADAS:** 

- (01) MySQL 5.7 Reference Manual https://dev.mysql.com/doc/refman/5.7/en/ 

   - https://downloads.mysql.com/docs/refman-5.7-en.pdf 

- (02) Los 7 pasos a seguir para el manejo de MySQL con Java http://panamahitek.com/los-7-pasos-seguir-para-el-manejo-de-mysql-con-java/ 

- (03) conexión Mysql con JAVA(netbeans) - YouTube https://www.youtube.com/watch?v=zJBI4pGylFELista de libros, artículos, etc. en formato IEEE 

- (04) VIDEO   Tutorial JTable en Java usando NetBeans y conectado a MySQL  -YouTube https://www.youtube.com/watch?v=wTjQ5HJMRHc 

- (05) VIDEO   Tutorial para implementar sobre cómo conectar una base de datos MySQL a Java usando el IDE NetBeans. 

   - https://youtu.be/4cS5I66MUHo 

## **POSTGRESQL** 

- (06) Cómo instalar PostgreSQL en Windows 10 + Cómo ejecutar PostgreSQL como una app de escritorio 

https://www.youtube.com/watch?v=RgP1njsQO0g 

- (07) Como Crear una Tabla | PostgreSQL #4 

   - https://www.youtube.com/watch?v=LIUW0XWdy80 

- (08) 5 horas y media| Curso completo de bases de datos en postgreSQL desde cero hasta avanzado Parte 1 

   - https://www.youtube.com/watch?v=HGfrzsGg3As 

- (09) POSTGRESQL Y PGADMIN TUTORIAL COMPLETO EN ESPAÑOL | BASES DE DATOS https://www.youtube.com/watch?v=rUZzng_Mr8c 

- (10) Crear base datos, tablas, campos, relaciones - Base de datos PostgreSQL – PROYECTO FINAL https://www.youtube.com/watch?v=PTs5v6WNfm4 

## **TÉCNICAS E INSTRUMENTOS DE EVALUACIÓN** 

**TÉCNICAS: Trabajo INSTRUMENTOS: Rubrica** 

## **CRITERIOS DE EVALUACIÓN** 

|Contenido|Excelente (10)|Bueno (8)|Regular (6)|Insuficiente (2)|
|---|---|---|---|---|
|Resumen:<br>Contexto,<br>Tema a investigar,<br>Objetivos,<br>Resultados,<br>Conclusiones|Se<br>redacta<br>excelentemente<br>el<br>resumen<br>contemplando<br>los<br>cinco aspectos más<br>importantes<br>dando<br>una visión general del<br>documento.|Se redacta bien el<br>resumen<br>contemplando<br>de<br>tres a cuatro de los<br>aspectos<br>más<br>importantes dando<br>una visión parcial<br>del documento.|Se redacta regular<br>el<br>resumen<br>contemplando<br>de<br>dos a tres de los<br>aspectos<br>más<br>importantes dando<br>una visión reducida<br>del documento.|El<br>resumen<br>presentado no aborda<br>mínimo los aspectos<br>necesarios no dando<br>una visión clara del<br>documento.|



## **UNIVERSIDAD NACIONAL DE SAN AGUSTIN FACULTAD DE INGENIERÍA DE PRODUCCIÓN Y SERVICIOS ESCUELA PROFESIONAL DE INGENIERÍA DE SISTEMA** 

||Introducción:<br>Contexto,<br>Tema de<br>investigación,<br>Objetivo,<br>Justificación,<br>Metodología,<br>Aportes,<br>Resultados,<br>Conclusiones.|Se aborda de forma<br>clara y puntual siete u<br>ocho de los elementos<br>de la introducción.|Se<br>abordan<br>con<br>claridad cinco o seis<br>de los elementos de<br>la introducción.|Se<br>abordan<br>con<br>claridad<br>tres<br>de<br>cuatro<br>de<br>los<br>elementos<br>de<br>la<br>introducción y el<br>resto corresponde a<br>otros temas.|La introducción<br>presentada no aborda<br>los elementos de la<br>introducción y lo<br>contemplado refleja<br>otros temas.||
|---|---|---|---|---|---|---|
||Objetivos:<br>Principal<br>Específicos|Se redactan de forma<br>clara manteniendo la<br>coherencia pertinente.|Se<br>redactan<br>de<br>forma clara, pero<br>manteniendo cierta<br>coherencia.|No se redactan de<br>forma clara y les<br>falta coherencia.|No se redactan de<br>forma clara no siendo<br>coherentes entre sí y<br>difícil de comprender.||
||Marco Teórico:<br>Conceptos Previos<br>Conceptos Nuevos<br>. Del Problema<br>. Técnicos<br>Estándares<br>Metodologías<br>Métodos<br>Técnicas<br>Trabajos<br>. Tesis<br>. Artículos<br>. Casos de Éxito<br>Herramientas|El<br>marco<br>teórico<br>contempla de forma<br>clara y puntual de<br>ocho a diez elementos<br>involucrados<br>adecuadamente<br>trabajados.|El marco teórico<br>contempla de forma<br>clara y puntual de<br>cinco<br>a<br>siete<br>elementos<br>involucrados<br>adecuadamente<br>trabajados.|El marco teórico<br>contempla de forma<br>clara y puntual de<br>tres<br>a<br>cuatro<br>elementos<br>involucrados<br>adecuadamente<br>trabajados.|El<br>marco<br>teórico<br>presentado contempla<br>un<br>marco<br>teórico<br>deficiente relacionado<br>a<br>los<br>elementos<br>involucrados.||
||Metodología:<br>Tipo de Investigación<br>Método Recopilación<br>de información<br>Universo<br>Muestra<br>Etapas del Proyecto<br>. Descripción<br>. Organización<br>. Definición Problema<br>. Cronograma|Se explica claramente<br>de<br>siete<br>a<br>ocho<br>elementos<br>involucrados de la<br>metodología<br>adecuadamente<br>trabajados<br>y<br>relacionados.|Se<br>explica<br>claramente de cinco<br>a seis elementos<br>involucrados de la<br>metodología<br>adecuadamente<br>trabajados<br>y<br>relacionados.|Se<br>explica<br>claramente de tres a<br>cuatro<br>elementos<br>involucrados de la<br>metodología<br>adecuadamente<br>trabajados<br>y<br>relacionados.|Los<br>elementos<br>involucrados<br>son<br>deficientes<br>al<br>ser<br>contemplados en la<br>metodología.||
||Resultados<br>(de<br>acuerdo<br>a<br>la<br>naturaleza<br>del<br>Proyecto)|Se<br>expresa<br>redactando<br>con<br>claridad los resultados<br>contrastando<br>los<br>mismos<br>con<br>lo<br>trabajado<br>en<br>el<br>proyecto.|Se<br>expresa<br>redactando<br>con<br>claridad hasta el<br>80%<br>de<br>los<br>resultados<br>contrastando<br>los<br>mismos<br>con<br>lo<br>trabajado<br>en<br>el<br>proyecto.|Se<br>expresa<br>redactando<br>con<br>claridad del 60% al<br>80%<br>de<br>los<br>resultados<br>contrastando<br>los<br>mismos<br>con<br>lo<br>trabajado<br>en<br>el<br>proyecto.|Los resultados no se<br>expresan redactando<br>con<br>claridad<br>ni<br>contrastando<br>los<br>mismos<br>con<br>lo<br>trabajado<br>en<br>el<br>proyecto.||
||Conclusiones|Se redactan de forma<br>clara y puntual en<br>concordancia a los<br>objetivos planteados<br>en el proyecto y de los<br>resultados obtenidos.|Se<br>redactan<br>de<br>forma<br>clara<br>y<br>puntual<br>en<br>concordancia<br>parcial<br>a<br>los<br>objetivos planteados<br>en el proyecto y bien<br>de los resultados<br>obtenidos.|Se<br>redactan<br>de<br>forma<br>clara<br>y<br>puntual<br>en<br>concordancia a los<br>objetivos planteados<br>en el proyecto y<br>parcialmente de los<br>resultados<br>obtenidos.|Las conclusiones no<br>se redactan de forma<br>clara y puntual así<br>como también de los<br>resultados obtenidos.||



**Página:** 14 

## **UNIVERSIDAD NACIONAL DE SAN AGUSTIN FACULTAD DE INGENIERÍA DE PRODUCCIÓN Y SERVICIOS ESCUELA PROFESIONAL DE INGENIERÍA DE SISTEMA** 

**Formato:** Guía de Práctica de Laboratorio / Talleres / Centros de Simulación 

|Recomendaciones|Las recomendaciones<br>se redactan de forma<br>clara y basadas en las<br>conclusiones.|Las<br>recomendaciones<br>se<br>redactan<br>parcialmente<br>de<br>forma clara y bien<br>basadas<br>en<br>las<br>conclusiones.|Las<br>recomendaciones<br>se<br>redactan<br>de<br>forma<br>clara<br>y<br>parcialmente<br>basadas<br>en<br>las<br>conclusiones.|Las recomendaciones<br>redactadas<br>no<br>corresponden<br>al<br>problema o no se<br>basan<br>en<br>las<br>conclusiones.|
|---|---|---|---|---|
|Referencias|Todas las referencias<br>cumplen con el estilo<br>APA o IEEE.|Del 60 al 80 % de las<br>referencias cumple<br>con el estilo APA o<br>IEEE.|Del 40 al 60 % de las<br>referencias cumplen<br>con el estilo APA o<br>IEEE.|Las referencias No<br>cumplen con el estilo<br>APA o IEEE|
|Anexos|Se incluyen anexos y<br>son citados dentro del<br>documento.|Se incluyen anexos<br>y<br>son<br>citados<br>parcialmente dentro<br>del documento.|Se incluyen anexos<br>y<br>son<br>citados<br>reducidamente<br>dentro<br>del<br>documento.|No se incluyen anexos<br>o se incluyen anexos y<br>no se citan dentro del<br>documento.|
|Informe de<br>Entregable|El informe presentado<br>tiene excelente<br>expresión escrita,<br>coherencia en la<br>redacción, estructura<br>y contenido|El informe<br>presentado tiene<br>muy buena<br>expresión escrita,<br>coherencia en la<br>redacción,<br>estructura y<br>contenido|El informe<br>presentado tiene<br>buena expresión<br>escrita, coherencia<br>en la redacción,<br>estructura y<br>contenido|El informe presentado<br>tiene regular<br>expresión escrita,<br>coherencia en la<br>redacción, estructura<br>y contenido|
|Para investigación preliminar|||||
|Autoevaluación|La<br>autoevaluación<br>general realizada por<br>cada miembro del<br>equipo<br>es<br>de<br>consistencia<br>excelente de acuerdo<br>a lo trabajado|La autoevaluación<br>general<br>realizada<br>por cada miembro<br>del equipo es de<br>consistencia<br>muy<br>buena de acuerdo a<br>lo trabajado|La autoevaluación<br>general<br>realizada<br>por cada miembro<br>del<br>equipo<br>es<br>consistencia regular<br>de acuerdo a lo<br>trabajado|La<br>autoevaluación<br>general realizada por<br>cada miembro del<br>equipo<br>es<br>inconsistente<br>de<br>acuerdo a lo trabajado|



Los logros se muestran en el Informe de Retroalimentación que se envía a los estudiantes en el más breve plazo para su análisis y toma de decisiones de mejoras a realizar. 

