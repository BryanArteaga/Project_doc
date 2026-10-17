# DocControl — Panel Web

Panel de administración clínica de **DocControl**. Permite a los doctores gestionar pacientes, revisar registros de salud y activar o desactivar cuentas.

Construido con **React + TypeScript + Vite** y **Tailwind CSS**.

## ✨ Funcionalidades

- 👨‍⚕️ Panel del doctor: pacientes pendientes y activos.
- 📊 Visualización de registros de salud (glucosa, colesterol, presión).
- ✅ Activación / desactivación de cuentas de pacientes.
- 🔄 Sincronización en tiempo real con la API y Supabase.

## 🚀 Inicio rápido

```bash
# Instalar dependencias
npm install

# Modo desarrollo
npm run dev
```

La aplicación abrirá en `http://localhost:5173`.

```bash
# Compilar para producción
npm run build

# Vista previa de la compilación
npm run preview

# Linting
npm run lint
```

## ⚙️ Configuración

Configura la URL de la API en una variable de entorno `.env`:

```env
VITE_API_URL=http://localhost:3000
```

> ⚠️ Las variables `VITE_*` quedan **incluidas en el bundle** del navegador. No pongas ahí secretos; usa solo valores públicos (URLs, IDs públicos).

## 📚 Documentación relacionada

- [Seguridad y auditoría](./SEGURIDAD_Y_AUDITORIA.md)
- [API Backend](../doc-backend/README.md)
