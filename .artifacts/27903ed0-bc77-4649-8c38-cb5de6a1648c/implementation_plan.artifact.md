# Plan de Desarrollo: StreamVault (JoeMovies) - Navegación y Vistas

Este plan detalla la implementación de las pantallas principales después del login, diferenciando entre Usuarios y Administradores.

## Resumen del Proyecto
Se implementará la lógica de redirección basada en roles. Los **Usuarios** verán un catálogo de películas (Home) y los **Administradores** verán un panel de gestión.

## Cambios Propuestos

### 1. Gestión de Roles y Navegación
*   **Redirección Dinámica**: `MainActivity` actuará como controlador inicial para verificar el rol del usuario autenticado y dirigirlo a la vista correspondiente.

#### [MODIFY] [MainActivity.java](file:///C:/Users/Eder/AndroidStudioProjects/JoeMovies/app/src/main/java/com/example/joemovies/MainActivity.java)
* Cambiar la lógica para que, si el rol es "ADMIN", inicie `AdminActivity`.
* Si es "USER", permanezca en `MainActivity` pero con la UI del catálogo.

#### [NEW] [AdminActivity.java](file:///C:/Users/Eder/AndroidStudioProjects/JoeMovies/app/src/main/java/com/example/joemovies/ui/admin/AdminActivity.java)
* Actividad principal para gestores.

#### [NEW] [activity_admin.xml](file:///C:/Users/Eder/AndroidStudioProjects/JoeMovies/app/src/main/res/layout/activity_admin.xml)
* Interfaz simplificada con estadísticas (Total títulos, películas, series) y botón para agregar contenido.

### 2. Diseño de Vistas (Simplificado)
*   **Inicio Usuario (Home)**: Estructura con banner destacado, buscador y categorías.

#### [MODIFY] [activity_main.xml](file:///C:/Users/Eder/AndroidStudioProjects/JoeMovies/app/src/main/res/layout/activity_main.xml)
* Implementar `BottomNavigationView` para navegación.
* Diseño oscuro con secciones de "Continuar viendo" y categorías.

## Plan de Verificación

### Pruebas Automatizadas
* N/A en esta fase.

### Verificación Manual
* Login como **USER** -> Debe cargar el catálogo de películas.
* Login como **ADMIN** -> Debe cargar el Panel de Administración.
* El botón de Cerrar Sesión debe funcionar en ambas vistas.
