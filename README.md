# Echo Guard · App de evacuación segura (Kotlin + Jetpack Compose + Material 3)

App de la brigada y la comunidad educativa para el sistema **Echo Guard** (rovers con sensores +
estaciones de pared). Grupo 3 · Tecsup.

> **Echo Guard** = *Echo* (el eco que guía la evacuación) + *Guard* (resguardo, seguridad).

### 📲 [Descargar la app (APK)](https://github.com/Jeanfranco-Flores-Valencia/Echo-Innova/releases/latest/download/echo-guard.apk)
Versión de prueba para Android 8.0+. Instrucciones de instalación y novedades en
[Releases](https://github.com/Jeanfranco-Flores-Valencia/Echo-Innova/releases).

| Inicio (alumno) | Roles y permisos (dirección) | Guía sin internet |
|:-:|:-:|:-:|
| ![Inicio](docs/capturas/inicio-alumno.png) | ![Roles y permisos](docs/capturas/roles-y-permisos.png) | ![Guía](docs/capturas/guia.png) |

## Novedades de la v2.0 (validación con el Cuerpo General de Bomberos)
Un especialista del Cuerpo General de Bomberos Voluntarios del Perú evaluó la app. Cada una de sus
recomendaciones se convirtió en una mejora:

| Recomendación del especialista | Qué hace ahora la app |
|---|---|
| Notificaciones inmediatas a todos los responsables, incluso en segundo plano | Servicio en primer plano que mantiene las alertas activas con la app minimizada o la pantalla apagada, y botón **"Confirmar recepción"** en la notificación |
| Saber quién recibió la alerta y quién no ha respondido | Panel **"Confirmaron X de Y"** en Alertas y en Evacuación: enviada, recibida o confirmada por cada responsable, con **"Reenviar a pendientes"** |
| Mapas interactivos con salidas, zonas seguras, escaleras y rutas | **Mapa de evacuación** con capas (salidas, zonas seguras, punto de reunión, escaleras, extintores, rovers), "Estás aquí" y el camino real por los pasillos desde tu aula |
| Rutas alternativas cuando una salida está bloqueada por humo o fuego | Si un rover detecta humo, fuego o gas, **cierra solo la salida más cercana** y la ruta se recalcula al instante; la alerta indica la alternativa |
| Mejorar la autonomía de los rovers | **Retorno automático** a la estación para cargar al 15 % de batería |
| Cámaras de visión nocturna y transmisión en tiempo real | Interruptor de **visión nocturna (IR)** en las cámaras en vivo |
| Sensores reales de temperatura, humo y gases | Contrato de **telemetría JSON** listo para recibir los datos del ESP32 (ver "Conectar el hardware real") |
| Niveles de acceso y evitar que no autorizados activen alertas | Matriz de permisos por rol (ya existía) + **confirmación con PIN** para iniciar o finalizar una evacuación real + límite a los reportes anónimos (1 por minuto) |
| Proteger la información personal y las ubicaciones | La ubicación del SOS es **opcional** (interruptor en Contactos SOS) y **no se guarda** en el historial del teléfono |
| Registros de las emergencias | **Historial de emergencias**: duración, quién inició y cerró, cuántas personas salieron y cuántos responsables confirmaron |
| Funciones principales sin conexión; SMS o radio | **SMS a responsables** al evacuar (dirección registra sus teléfonos), aviso **"Sin internet"** en Inicio; SOS, mapa, guía y conteo funcionan sin datos |

También se atendió lo que pidieron los usuarios en la primera prueba: **la ruta ahora está en Inicio**
y hay menos texto en pantalla.

## Cómo abrir
1. Android Studio (Ladybug o superior) → **Open** → la carpeta del repositorio.
2. Esperar la sincronización de Gradle → **Run** en emulador o celular (Android 8.0+).
3. **Primera vez:** registro único (nombre + "Alumno" con su aula, o "Visitante"). Sin contraseña.
   El perfil queda guardado en el teléfono: al volver a abrir la app entra directo a Inicio.
4. **Personal:** botón "Personal" en Inicio (o "Soy docente o brigadista" en el registro) → PIN.

También puedes descargar el APK listo para instalar desde
[Releases](https://github.com/Jeanfranco-Flores-Valencia/Echo-Innova/releases).

| Modo | Cómo se entra | ¿Se mantiene al cerrar la app? |
|---|---|---|
| Alumno / Visitante | Registro único | ✅ Sí, siempre |
| Docente (aula 3B) | PIN 3333 (demo) | ❌ Al reabrir vuelve al perfil regular |
| Brigadista | PIN 1111 (demo) | ❌ |
| Dirección | PIN 2222 (demo) | ❌ |

## Roles y permisos
Todo se decide en **una sola matriz** (`Permission` × `Role` en `data/model/Models.kt`). Las pantallas
la usan para mostrar u ocultar acciones, y **el repositorio la vuelve a validar** en cada acción
sensible: aunque una pantalla mostrara un botón por error, la acción no se ejecutaría.

| Acción | Brigadista | Director | Docente | Alumno / Visitante |
|---|:-:|:-:|:-:|:-:|
| Iniciar / finalizar evacuación general | ✅ | ✅ | ❌ (solicita) | ❌ (reporta) |
| Controlar rovers (desplegar, luz, cámara) | ✅ | 👁 solo lectura | ❌ | ❌ |
| Ver flota, mapa, rango y última ubicación | ✅ | ✅ | ❌ | ❌ |
| Cámaras en vivo | ✅ | ✅ | ❌ | ❌ |
| Ver y resolver detecciones y reportes | ✅ | ✅ | ❌ | ❌ |
| Reportar estado de salidas | ✅ | ❌ | ❌ | ❌ |
| Pasar lista de su aula | — | — | ✅ | ❌ |
| Tablero de aulas, asistencia y "a salvo" | ✅ | ✅ | ❌ | ❌ |
| Crear / desactivar personal y cambiar PIN | ❌ | ✅ | ❌ | ❌ |
| Ver registro de acciones (auditoría) | ❌ | ✅ | ❌ | ❌ |
| Ver resultados de las pruebas y reiniciar la demo | ✅ | ✅ | ❌ | ❌ |
| Ver ruta, guía de emergencias, "Estoy a salvo" | ✅ | ✅ | ✅ | ✅ |
| Reportar emergencia / pedir ayuda / SOS a contactos | ✅ | ✅ | ✅ | ✅ (reportar también sin registrarse) |

La matriz completa también se ve en la app: **Inicio → Roles y permisos**.

**Seguridad del modo personal**
- Los PIN se guardan **solo como hash SHA-256** en la base de datos, nunca en texto plano.
- **5 intentos fallidos → bloqueo de 30 s.**
- El modo personal **no se guarda** al cerrar la app y **se cierra solo tras 10 min sin uso**,
  excepto durante una evacuación (para no dejar a la brigada fuera en plena emergencia).
- Dirección administra las cuentas: crear personas (con aula para docentes), desactivarlas y
  cambiar PIN. No puede desactivarse a sí misma ni dejar el colegio sin una cuenta de dirección.
- Cada acción sensible (iniciar o finalizar una evacuación, desplegar rovers, resolver alertas,
  pasar lista, enviar SOS, cambios de personal...) queda en el **registro de acciones**.

Las detecciones de sensores y los reportes solo llegan a quien decide (brigada y dirección), para
evitar pánico por falsas alarmas. La evacuación, en cambio, llega a todos.

## SOS a contactos de emergencia (funciona sin internet)
**Inicio → mantener presionado SOS** (1,5 s, para evitar envíos accidentales) → elegir qué pasa
(*Estoy a salvo · Necesito ayuda · Estoy herido(a) · Estoy atrapado(a)*) → **Enviar**.

El mensaje se envía por **SMS**: no necesita datos ni wifi, solo señal del operador. Incluye:
- Nombre, rol y aula.
- **Ubicación GPS** con enlace a Google Maps, precisión y antigüedad. El GPS funciona sin internet.
- **Batería** (% y si está cargando, en ahorro o por agotarse).
- Si hay una **evacuación en curso**, su causa y la **ruta recomendada**.
- Fecha y hora.

El texto va sin tildes para que entre en menos SMS y se lea en cualquier teléfono. Si la app no tiene
permiso de SMS (o el teléfono no tiene SIM), abre la app de mensajes con todo ya escrito. Puedes
guardar hasta 5 contactos (a mano o elegidos de la agenda, sin dar permiso a todos tus contactos)
y queda un historial de los últimos SOS.

## Probar la app con usuarios (opiniones)
**Inicio → Probar y opinar.** Es una prueba guiada de unos 5 minutos:
1. **Tareas según el rol** (por ejemplo: encontrar la ruta, enviar un SOS, iniciar un simulacro, pasar
   lista). La persona marca *Fácil / Me costó / No pude / No la probé*.
2. **4 preguntas de 1 a 5** (facilidad, claridad, confianza en una emergencia real, diseño) y
   **recomendación de 0 a 10** (NPS).
3. **Comentarios**: qué le gustó, qué mejoraría, qué falló.

Las respuestas se guardan en la **base de datos del teléfono** (Room/SQLite) y funcionan sin internet.
- **Exportar CSV** (se abre en Excel o Google Sheets) y compartir por WhatsApp, correo o Drive.
- Brigada y dirección ven los **resultados**: promedios, NPS, % de éxito por tarea (las más
  difíciles primero) y los comentarios recientes.
- **Siguiente participante**: reinicia la demo y borra el perfil para que la siguiente persona se
  registre (las respuestas se conservan). Ideal para pruebas con un mismo teléfono.
- **Opcional, envío automático a Google Sheets**: sigue `docs/feedback-apps-script.gs` y agrega
  `feedbackUrl=https://script.google.com/macros/s/XXXX/exec` a `local.properties`. Si no hay
  internet, la app guarda la respuesta y la envía después.

## Interfaz inspirada en apps reales de emergencia
- **Estado claro en Inicio**: "Todo en orden" o la señal de evacuación con la ruta, en grande.
- **"Estoy a salvo"** durante una evacuación (como el *Safety Check*): un toque. La brigada ve
  cuántas personas y quiénes se marcaron a salvo.
- **SOS de mantener presionado** con anillo de progreso (como *Personal Safety* o *AlertCops*).
  Con TalkBack basta un doble toque.
- **Guía de emergencias sin internet** (sismo, incendio, humo, gas: antes, durante y después) y
  **mochila de emergencia** de INDECI con checklist.
- Botones de al menos 48–60 dp, alto contraste, tema rojo inconfundible en una emergencia real,
  vibración en las acciones críticas y llamada rápida al 116 / 106 / 105.

## Arquitectura (MVVM)
```
com.floresvalencia.echoguard
├── AppGraph            Dependencias (inyección manual): base de datos y repositorios
├── data/model          Roles, Permission (matriz única), Rover, Alert, Classroom, SafeMark...
├── data/repository     EchoRepository (estado en vivo, simulación y validación de permisos)
│                       StaffRepository (cuentas de personal + auditoría en Room)
├── data/db             Room: personal, auditoría, opiniones, contactos y SOS
├── data/feedback       Tareas por rol, estadísticas, CSV/JSON y envío opcional
├── data/sos            Mensaje SOS, SMS sin internet, GPS y batería
├── notification        AlarmNotifier: notificación de alarma a pantalla completa
├── ui/components       Cabeceras, botones grandes, SOS de mantener presionado, plano del edificio
├── ui/navigation       Pestañas por rol, NavHost, AppViewModel (sesión, alarmas, avisos)
└── ui/screens          access · home · monitor · fleet · evacuation · classroom · route · alerts
                        permissions · feedback · sos · guide
```

## Probar el flujo completo
1. Regístrate como **alumno** → **Contactos SOS** → agrega un número → **mantén presionado SOS**.
2. "Reportar" → Fuego. (No puede evacuar: solo avisa.)
3. "Personal" → PIN 1111 (**brigadista**) → aparece la alarma → "Iniciar evacuación" → Simulacro.
4. "Salir" → vuelves a tu perfil de alumno → **"Estoy a salvo"**.
5. "Personal" → PIN 2222 (**dirección**) → Evacuación → Aulas: ves quién está a salvo.
   **Roles y permisos** → crea un docente nuevo y revisa el **registro de acciones**.
6. "Personal" → PIN 3333 (**docente**) → pasa lista en "Mi aula".
7. **Probar y opinar** → completa la prueba → "Siguiente participante".

## Desarrollo
```bash
./gradlew testDebugUnitTest   # pruebas unitarias (permisos, sesión, SOS, estadísticas)
./gradlew lintDebug           # lint de Android
./gradlew assembleDebug       # APK en app/build/outputs/apk/debug/
```
GitHub Actions corre las tres en cada push a `main` (`.github/workflows/android.yml`).

**Publicar una versión nueva:** sube `versionCode` y `versionName` en `app/build.gradle.kts`, haz
commit y crea un tag (`git tag v1.2 && git push origin v1.2`, o desde GitHub → *Releases → Draft a new
release*), o ve a *Actions → Publicar versión → Run workflow* y escribe la versión. El workflow
`release.yml` compila, prueba y publica `echo-guard.apk` en Releases.

El esquema de la base de datos se versiona en `app/schemas/`. Si cambias una entidad, sube la
`version` de `EchoDatabase` y agrega una migración.

## Sensores: qué mide cada parte
| Dónde | Sensores | Por qué ahí |
|---|---|---|
| **Rover** (móvil) | Humo, monóxido de carbono, temperatura, polvo, llama y cámara orientable | Recorre los pasillos y mide donde está el peligro |
| **Estación de pared** (fija) | Sismo (acelerómetro) | Está anclada a la estructura: los motores y orugas del rover darían falsas alarmas |

## Conectar el hardware real
Cada rover (ESP32) y cada estación envían un JSON plano; la app lo procesa con `Telemetry.parse()` y
`EchoRepository.applyTelemetry()`, sin tocar las pantallas:

```json
{"rover":"R-01","battery":76,"smoke":12,"co":4,"temp":24,"dust":18,"flame":false,"x":20.5,"y":17}
{"station":"E1","seismic":0.02}
```

Todos los campos salvo el id son opcionales, y un mensaje inválido se ignora sin afectar la app. El
transporte (MQTT o HTTP en la red local) es lo único que falta elegir cuando el hardware esté listo.
Los umbrales de sensores están en `SensorReadings` (rover) y `StationReadings` (estación) y deben
calibrarse con los sensores reales.

**Pendiente para producción:**
- Las alertas entre teléfonos distintos necesitan un servidor o notificaciones push (FCM). Hoy el
  servicio en primer plano mantiene las alertas activas en el teléfono donde se generan.
- Los PIN del personal deberían validarse en un servidor.
- Para comunicación por radio cuando no hay señal celular, se recomienda agregar un módulo LoRa a las
  estaciones (de unos kilómetros de alcance, sin internet ni operador).
