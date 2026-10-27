# BAYMAX-ECHO · App móvil (Kotlin + Jetpack Compose + Material 3)

App de la brigada para el sistema BAYMAX-ECHO (rover + estación de pared). Grupo 3 · Tecsup.

## Cómo abrir
1. Android Studio (Ladybug o superior) → **Open** → carpeta `BaymaxEcho`.
2. Esperar la sincronización de Gradle → **Run** en emulador o celular (Android 8.0+).
3. **Primera vez:** registro único (nombre + "Alumno" con su aula, o "Visitante"). Sin contraseña.
   El perfil queda guardado en el teléfono: al volver a abrir la app entra directo a Inicio.
4. **Personal:** botón "Personal" en Inicio (o "Soy docente o brigadista" en el registro) → PIN.

| Modo | Cómo se entra | ¿Se mantiene al cerrar la app? |
|---|---|---|
| Alumno / Visitante | Registro único | ✅ Sí, siempre |
| Docente (aula 3B) | PIN 3333 | ❌ Al reabrir vuelve al perfil regular |
| Brigadista | PIN 1111 | ❌ |
| Dirección | PIN 2222 | ❌ |

El modo personal no se guarda a propósito: si un teléfono se pierde o se presta, no conserva
permisos para iniciar evacuaciones. "Salir" vuelve al perfil regular sin borrar el registro.

## Permisos por rol

| Acción | Brigadista | Director | Docente | Alumno / Visitante |
|---|:-:|:-:|:-:|:-:|
| Iniciar / finalizar evacuación general | ✅ | ✅ | ❌ (solicita) | ❌ (reporta) |
| Controlar rovers (desplegar, luz, cámara) | ✅ | 👁 solo lectura | ❌ | ❌ |
| Ver flota, mapa, rango y última ubicación | ✅ | ✅ | ❌ | ❌ |
| Cámaras en vivo | ✅ | ✅ | ❌ | ❌ |
| Reportar estado de salidas | ✅ | ❌ | ❌ | ❌ |
| Pasar lista de su aula | — | — | ✅ | ❌ |
| Tablero de aulas y personas con asistencia | ✅ | ✅ | ❌ | ❌ |
| Ver ruta de evacuación | ✅ | ✅ | ✅ | ✅ |
| Reportar emergencia / pedir ayuda | ✅ | ✅ | ✅ | ✅ (también sin registrarse) |

Las detecciones de sensores y los reportes solo llegan a quien decide (brigada y dirección),
para evitar pánico por falsas alarmas. La evacuación, en cambio, llega a todos.

## Arquitectura (MVVM)
```
com.floresvalencia.baymaxecho
├── data/model          Roles y permisos, Rover, SensorReadings, Station, ExitRoute, Classroom, Alert...
├── data/repository     EchoRepository: fuente única (StateFlow). Simula flota, sensores y batería.
├── notification        AlarmNotifier: notificación de alarma a pantalla completa
├── ui/theme            Paleta navy, verde de seguridad, tema de emergencia real
├── ui/components       Cabecera, botones grandes, señal de evacuación, plano del edificio, alarma
├── ui/navigation       Pestañas por rol, NavHost, AppViewModel (sesión, alarmas, banner)
└── ui/screens          access (registro + PIN) · home · monitor · fleet (flota + detalle) · evacuation · classroom · route · alerts
```

## Flota de rovers
- Disponibles / desplegados / cargando / sin señal, con plano del edificio.
- **Rango operativo** de cada estación (círculo punteado) y aviso si un rover sale del rango.
- **Última ubicación registrada** cuando un rover se queda sin batería o sin señal: marcador
  punteado en el mapa, zona, coordenadas, hace cuánto y los últimos valores de sensores.
- Sensores por rover: humo, CO, temperatura, sismo, polvo y llama (con umbrales de alarma).
- Demo: simular sismo / humo / gas / fuego, agotar batería o cortar la señal.

## Probar el flujo completo
1. Regístrate como **alumno** → "Reportar" → Fuego. (No puede evacuar: solo avisa.)
2. "Personal" → PIN 1111 (**brigadista**) → aparece la alarma → "Iniciar evacuación" → Real.
3. "Salir" → vuelves a tu perfil de alumno y ves la señal de salida.
4. "Personal" → PIN 3333 (**docente**) → pasa lista en "Mi aula".
5. Cierra la app por completo y ábrela: entra directo con tu perfil de alumno, sin pedir nada.

## Conectar el hardware real
Reemplazar el cuerpo de las funciones de `EchoRepository` (MQTT, Firebase o HTTP al ESP32).
Los umbrales de sensores están en `SensorReadings` y deben calibrarse con los sensores reales.
