# 📺 BarBanner - Sistema de Avisos para Bares y Restaurantes

![Platform](https://img.shields.io/badge/Platform-Android-green.svg)
![Language](https://img.shields.io/badge/Language-Kotlin-purple.svg)
![License](https://img.shields.io/badge/License-MIT-blue.svg)
![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)

Sistema completo de banners informativos para TVs en bares, restaurantes y negocios. Controla y actualiza mensajes en pantallas de TV en tiempo real desde tu smartphone.

## ✨ Características

### 📱 App Mobile (Control)
- 🔍 **Descubrimiento automático** de TVs en la red local (mDNS/NSD)
- 🎨 **15 temas visuales** (Navidad, Verano, Halloween, etc.)
- ⚡ **Actualización en tiempo real** vía WebSocket
- 💾 **Persistencia de preferencias** (último mensaje, tipo, frecuencia)
- 🎯 **6 tipos de avisos**: Ofertas, Tapas, Eventos, Lotería, Personal, Otros
- 🔄 **Control de repeticiones**: 1 vez, 2 veces, 3 veces o continuo
- 🎭 **Efectos especiales** opcionales

### 📺 App TV (Display)
- 🖥️ **Overlay transparente** sobre contenido de TV (SYSTEM_ALERT_WINDOW)
- 🌐 **Servidor WebSocket** integrado (puerto 8080)
- 🎨 **Banners con colores dinámicos** según tipo de aviso
- ✨ **Animaciones fluidas** con Jetpack Compose
- 🔔 **Servicio en primer plano** (Foreground Service)
- 📡 **Registro automático** en la red (Network Service Discovery)

## 🏗️ Arquitectura

```
┌─────────────────┐         WebSocket (8080)        ┌─────────────────┐
│                 │◄──────────────────────────────► │                 │
│  App Mobile     │     mDNS/NSD Discovery          │    App TV       │
│  (Control)      │◄──────────────────────────────► │   (Display)     │
│                 │                                 │                 │
└─────────────────┘                                 └─────────────────┘
   • Jetpack Compose                                  • Compose for TV
   • Ktor Client                                      • Ktor Server
   • Material 3                                       • Window Overlay
   • DataStore Preferences                            • Foreground Service
```

## 🚀 Instalación

### Requisitos Previos
- Android Studio Hedgehog | 2023.1.1 o superior
- Kotlin 1.9.22+
- Gradle 8.2+
- Android SDK 34
- Dispositivo Android TV o Android con API 26+

### Clonar el Repositorio

```bash
git clone https://github.com/charran78/barbanner.git
cd barbanner
```

### Compilar las Apps

#### App Mobile:
```bash
cd barbanner-mobile
./gradlew assembleDebug
```

#### App TV:
```bash
cd barbanner-tv
./gradlew assembleDebug
```

### Instalar en Dispositivos

```bash
# Instalar en smartphone
adb -d install app/build/outputs/apk/debug/app-debug.apk

# Instalar en TV (conectada por red)
adb connect <IP_DE_LA_TV>:5555
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📖 Uso

### 1️⃣ Configurar la TV

1. Instala la **App TV** en tu Android TV o dispositivo Android
2. Otorga el permiso de "Mostrar sobre otras apps" (SYSTEM_ALERT_WINDOW)
3. La app iniciará automáticamente el servicio de banner
4. Verás la IP de la TV en pantalla (ej: `192.168.1.100:8080`)

### 2️⃣ Configurar el Smartphone

1. Instala la **App Mobile** en tu smartphone
2. Asegúrate de estar en la **misma red WiFi** que la TV
3. La app descubrirá automáticamente las TVs disponibles
4. Selecciona tu TV de la lista

### 3️⃣ Publicar Banners

1. Escribe tu mensaje en el campo de texto
2. Selecciona el tipo de aviso (Oferta, Tapa, Evento, etc.)
3. Elige la frecuencia (1 vez, 2 veces, 3 veces o continuo)
4. Activa/desactiva efectos especiales
5. Presiona **"PUBLICAR EN TV"**
6. ¡El banner aparecerá inmediatamente en la TV! 🎉

### 4️⃣ Personalizar Tema

1. En la pantalla de descubrimiento, presiona **"Cambiar Tema"**
2. Selecciona uno de los 15 temas disponibles
3. El tema se aplicará inmediatamente y se guardará para futuras sesiones

## 🎨 Tipos de Avisos

| Tipo          | Color         | Icono     |   Uso Recomendado         |
|---------------|---------------|-----------|---------------------------|
| **OFERTA**    | 🟠 Naranja    | 🔥        | Promociones y descuentos  |
| **TAPA**      | 🟢 Verde      | 🍽️        | Tapas y platos del día    |
| **LOTERÍA**   | 🔴 Rojo       | 🎰        | Lotería disponible        |
| **EVENTO**    | 🔵 Azul       | ⚽        | Eventos deportivos        |
| **PERSONAL**  | 🟣 Morado     | 🎉        | Celebraciones personales  |
| **OTROS**     | ⚫ Gris       | 📢        | Avisos generales          |

## 🎭 Temas Disponibles

- 🎄 **Navidad** - Rojo, verde y dorado
- 👑 **Reyes Magos** - Morado, azul y amarillo
- ❤️ **San Valentín** - Tonos rosa
- 🎭 **Carnaval** - Morado y lila
- ✝️ **Semana Santa** - Azul índigo
- 🐣 **Pascua** - Verde primavera
- 🌸 **Primavera** - Verde, amarillo y rosa
- ☀️ **Verano** - Cian y turquesa
- 🍂 **Otoño** - Naranja y marrón
- ❄️ **Invierno** - Azul helado
- 🎃 **Halloween** - Morado, negro y naranja
- 🕯️ **Día de Difuntos** - Grises
- 🇪🇸 **Hispanidad** - Rojo y amarillo
- 🎉 **Festivo** - Gris azulado
- 📱 **Default** - Azul profesional

## 🛠️ Tecnologías Utilizadas

### Mobile
- **Jetpack Compose** - UI moderna y declarativa
- **Material 3** - Diseño Material Design
- **Ktor Client** - Cliente WebSocket
- **Network Service Discovery (NSD)** - Descubrimiento automático
- **DataStore Preferences** - Persistencia de datos
- **Kotlinx Serialization** - Serialización JSON

### TV
- **Compose for TV** - UI optimizada para TV
- **Ktor Server** - Servidor WebSocket
- **WindowManager** - Overlay sobre otras apps
- **Foreground Service** - Servicio persistente
- **Kotlinx Serialization** - Deserialización JSON

## 📂 Estructura del Proyecto

```
barbanner/
├── barbanner-mobile/          # App móvil (control)
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/.../mobile/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── ThemeManager.kt
│   │   │   │   └── BannerAction.kt
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle.kts
│   └── build.gradle.kts
│
└── barbanner-tv/              # App TV (display)
    ├── app/
    │   ├── src/main/
    │   │   ├── java/.../tv/
    │   │   │   ├── MainActivity.kt
    │   │   │   ├── BannerService.kt
    │   │   │   └── BannerAction.kt
    │   │   └── AndroidManifest.xml
    │   └── build.gradle.kts
    └── build.gradle.kts
```

## 🔧 Configuración Avanzada

### Cambiar el Puerto del Servidor (TV)

En `BannerService.kt`:
```kotlin
companion object {
    const val SERVER_PORT = 8080 // Cambia este valor
}
```

### Ajustar Duración del Banner

En `BannerService.kt`:
```kotlin
companion object {
    const val BANNER_DISPLAY_DURATION_MS = 5000L // 5 segundos
}
```

### Personalizar Colores de Avisos

En `BannerService.kt`, método `BannerDisplay()`:
```kotlin
val (backgroundColor, textColor) = when (banner.type) {
    "OFERTA" -> Color(0xFFFF9800) to Color.White
    // Añade o modifica colores aquí
}
```

## 🐛 Solución de Problemas

### La TV no aparece en la lista

1. Verifica que ambos dispositivos estén en la **misma red WiFi**
2. Asegúrate de que el **servicio de banner** esté activo en la TV
3. Reinicia la app mobile y espera unos segundos
4. Verifica que el **firewall** no esté bloqueando el puerto 8080

### El banner no se muestra en la TV

1. Otorga el permiso de **"Mostrar sobre otras apps"** en la TV
2. Verifica que el **servicio esté activo** (notificación visible)
3. Revisa los **logs de Android Studio** para errores
4. Reinicia el servicio desde la actividad principal

### Error de conexión WebSocket

1. Verifica que la **IP y puerto** sean correctos
2. Comprueba que no haya **conflicto de puertos**
3. Reinicia el **servicio de banner** en la TV
4. Asegúrate de que ambas apps tengan permisos de **INTERNET**

## 📝 Logs de Depuración

### Ver logs de la app TV:
```bash
adb logcat | grep "BannerService"
```

### Ver logs de la app mobile:
```bash
adb logcat | grep "BARBANNER"
```

## 🤝 Contribuir

¡Las contribuciones son bienvenidas! Si quieres mejorar BarBanner:

1. Haz un **Fork** del proyecto
2. Crea una **rama** para tu feature (`git checkout -b feature/nueva-funcionalidad`)
3. **Commit** tus cambios (`git commit -m 'Añadir nueva funcionalidad'`)
4. **Push** a la rama (`git push origin feature/nueva-funcionalidad`)
5. Abre un **Pull Request**

### Ideas para Contribuir

- 🎨 Añadir más temas visuales
- 🌍 Soporte multiidioma
- 📊 Panel de estadísticas de mensajes
- 🔔 Notificaciones programadas
- 🎵 Efectos de sonido
- 📸 Soporte para imágenes en banners
- 🔐 Sistema de autenticación
- ☁️ Sincronización en la nube

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Consulta el archivo [LICENSE](LICENSE) para más detalles.

## 👨‍💻 Autor

© Pedro Mencías - 2025 - Asturias!

Desarrollado con ❤️ para la comunidad de hostelería

## 🙏 Agradecimientos

- **Jetpack Compose** por la increíble UI declarativa
- **Ktor** por el potente framework de networking
- **Android TV** por las APIs de overlay
- La comunidad de **Kotlin** por el excelente lenguaje

## 📞 Contacto

- 🐛 **Issues**: [GitHub Issues](https://github.com/charran78/barbanner/issues)
- 💬 **Discusiones**: [GitHub Discussions](https://github.com/charran78/barbanner/discussions)
- 📧 **Email**: beyond.digital.web@gmail.com

---

⭐ **¡Si te gusta este proyecto, dale una estrella en GitHub!** ⭐