# Resumen de Cambios: Pop-up Emergente, Vigencia de Publicidad y Gestión Independiente de Estrenos

Se han integrado todas las mejoras solicitadas para optimizar la experiencia de administración y usuario:

1. **Gestión Independiente de Estrenos (`ManagePremieresActivity`):**
   - Creada una pantalla exclusiva en el panel de administrador para seleccionar películas, marcarlas como "Estreno del Mes", configurar el **tiempo de vigencia en cartelera (días de publicidad)** y añadir cines aliados sin mezclarlo con el formulario general de películas.

2. **Ventana Emergente Automática al Ingresar (`dialog_premiere_popup.xml` & `MainActivity`):**
   - Al iniciar sesión como usuario, aparece inmediatamente una **ventana emergente modal** con la reproducción automática del tráiler en video (`VideoView`) del estreno vigente, título, descripción y botón directo **"📍 Ver Cines y Horarios de Función"**.
   - Incluye un temporizador regresivo de 5 segundos con botón **"Omitir"** para que el usuario pueda cerrar el pop-up en cualquier momento.

3. **Vigencia Automática de Publicidad:**
   - Mediante `premiereExpirationTimestamp` y `isPremiereActive()`, las películas en estreno expiran automáticamente al cumplirse los días configurados por el administrador, retirándose de los pop-ups y del mapa de cines.

4. **Carrusel con Rotación Automática en Inicio:**
   - La sección de estrenos e inicio rota automáticamente los pósters cada 4 segundos mediante un `Handler` en `MainActivity`.

---

## Verificación
- **Compilación exitosa:** Ejecutado `gradle_build("app:assembleDebug")` confirmando compilación limpia (`Build finished successfully`).
