-- CreateEnum
CREATE TYPE "UserRole" AS ENUM ('DOCTOR', 'PATIENT');

-- CreateEnum
CREATE TYPE "AccountStatus" AS ENUM ('INACTIVE', 'ACTIVE', 'SUSPENDED');

-- CreateEnum
CREATE TYPE "ExerciseAssignmentStatus" AS ENUM ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED');

-- CreateEnum
CREATE TYPE "NotificationType" AS ENUM ('account_activated', 'exercise_assigned', 'exercise_reminder', 'health_check', 'message_from_doctor');

-- CreateTable
CREATE TABLE "usuarios" (
    "id" SERIAL NOT NULL,
    "email" TEXT NOT NULL,
    "password" TEXT NOT NULL,
    "nombre" TEXT NOT NULL,
    "apellido" TEXT NOT NULL,
    "cedula" TEXT,
    "telefonoPersonal" TEXT,
    "fechaNacimiento" TIMESTAMP(3),
    "edad" INTEGER,
    "peso" DOUBLE PRECISION,
    "altura" DOUBLE PRECISION,
    "sexo" TEXT,
    "enfermedad" TEXT,
    "colorEnfermedad" TEXT,
    "rol" "UserRole" NOT NULL DEFAULT 'PATIENT',
    "estadoCuenta" "AccountStatus" NOT NULL DEFAULT 'INACTIVE',
    "idDoctor" TEXT,
    "idDoctorAsignado" INTEGER,
    "glucosaInicial" DOUBLE PRECISION,
    "colesterolInicial" DOUBLE PRECISION,
    "notifEnabled" BOOLEAN NOT NULL DEFAULT true,
    "notifType" TEXT NOT NULL DEFAULT 'daily_fixed',
    "notifDetails" TEXT NOT NULL DEFAULT '{"hour":20}',
    "activadoEn" TIMESTAMP(3),
    "activadoPor" INTEGER,
    "registradoEn" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "ultimaSincronizacion" TIMESTAMP(3),
    "tokenDispositivo" TEXT,
    "creadoEn" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "actualizadoEn" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "usuarios_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "registros_salud" (
    "id" SERIAL NOT NULL,
    "usuarioId" INTEGER NOT NULL,
    "presionSistolica" INTEGER,
    "presionDiastolica" INTEGER,
    "glucosa" DOUBLE PRECISION,
    "colesterol" DOUBLE PRECISION,
    "temperatura" DOUBLE PRECISION,
    "peso" DOUBLE PRECISION,
    "calorias" INTEGER,
    "ejercicio" INTEGER,
    "estadoAnimo" TEXT,
    "notas" TEXT,
    "idSincronizacion" TEXT,
    "sincronizadoEn" TIMESTAMP(3),
    "fecha" TEXT,
    "hora" TEXT,
    "registradoEn" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "creadoEn" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "actualizadoEn" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "registros_salud_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "ejercicios" (
    "id" SERIAL NOT NULL,
    "nombre" TEXT NOT NULL,
    "descripcion" TEXT,
    "enfermedad" TEXT,
    "duracion" INTEGER,
    "intensidad" TEXT,
    "imagenResId" INTEGER,
    "series" INTEGER NOT NULL DEFAULT 3,
    "descansoEntreSeries" INTEGER NOT NULL DEFAULT 30,
    "urlImagen" TEXT,
    "urlVideo" TEXT,
    "creadoEn" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "actualizadoEn" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "ejercicios_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "asignaciones_ejercicio" (
    "id" SERIAL NOT NULL,
    "ejercicioId" INTEGER NOT NULL,
    "pacienteId" INTEGER NOT NULL,
    "estado" "ExerciseAssignmentStatus" NOT NULL DEFAULT 'PENDING',
    "progreso" INTEGER NOT NULL DEFAULT 0,
    "frecuenciaRecordatorio" INTEGER,
    "asignadoEn" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "fechaInicio" TIMESTAMP(3),
    "fechaFin" TIMESTAMP(3),
    "idSincronizacion" TEXT,
    "sincronizadoEn" TIMESTAMP(3),
    "creadoEn" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "actualizadoEn" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "asignaciones_ejercicio_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "ejercicios_realizados" (
    "id" SERIAL NOT NULL,
    "usuarioId" INTEGER NOT NULL,
    "ejercicioId" INTEGER NOT NULL,
    "asignacionId" INTEGER,
    "duracionReal" INTEGER,
    "dificultad" INTEGER,
    "dolor" INTEGER,
    "notas" TEXT,
    "idSincronizacion" TEXT,
    "sincronizadoEn" TIMESTAMP(3),
    "fecha" TEXT,
    "completadoEn" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "creadoEn" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "ejercicios_realizados_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "consejos" (
    "id" SERIAL NOT NULL,
    "enfermedad" TEXT NOT NULL,
    "consejo" TEXT NOT NULL,
    "creadoEn" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "actualizadoEn" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "consejos_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "notificaciones" (
    "id" SERIAL NOT NULL,
    "usuarioId" INTEGER NOT NULL,
    "tipo" "NotificationType" NOT NULL,
    "titulo" TEXT NOT NULL,
    "mensaje" TEXT NOT NULL,
    "metadatos" JSONB,
    "leido" BOOLEAN NOT NULL DEFAULT false,
    "leidoEn" TIMESTAMP(3),
    "creadoEn" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "actualizadoEn" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "notificaciones_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "configuracion_sistema" (
    "id" SERIAL NOT NULL,
    "clave" TEXT NOT NULL,
    "valor" TEXT NOT NULL,
    "descripcion" TEXT,
    "creadoEn" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "actualizadoEn" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "configuracion_sistema_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "usuarios_email_key" ON "usuarios"("email");

-- CreateIndex
CREATE INDEX "usuarios_email_idx" ON "usuarios"("email");

-- CreateIndex
CREATE INDEX "usuarios_estadoCuenta_idx" ON "usuarios"("estadoCuenta");

-- CreateIndex
CREATE INDEX "usuarios_rol_idx" ON "usuarios"("rol");

-- CreateIndex
CREATE INDEX "usuarios_idDoctorAsignado_idx" ON "usuarios"("idDoctorAsignado");

-- CreateIndex
CREATE INDEX "registros_salud_usuarioId_registradoEn_idx" ON "registros_salud"("usuarioId", "registradoEn");

-- CreateIndex
CREATE INDEX "registros_salud_idSincronizacion_idx" ON "registros_salud"("idSincronizacion");

-- CreateIndex
CREATE INDEX "ejercicios_enfermedad_idx" ON "ejercicios"("enfermedad");

-- CreateIndex
CREATE INDEX "asignaciones_ejercicio_pacienteId_estado_idx" ON "asignaciones_ejercicio"("pacienteId", "estado");

-- CreateIndex
CREATE INDEX "asignaciones_ejercicio_idSincronizacion_idx" ON "asignaciones_ejercicio"("idSincronizacion");

-- CreateIndex
CREATE INDEX "ejercicios_realizados_usuarioId_completadoEn_idx" ON "ejercicios_realizados"("usuarioId", "completadoEn");

-- CreateIndex
CREATE INDEX "ejercicios_realizados_idSincronizacion_idx" ON "ejercicios_realizados"("idSincronizacion");

-- CreateIndex
CREATE INDEX "notificaciones_usuarioId_leido_idx" ON "notificaciones"("usuarioId", "leido");

-- CreateIndex
CREATE INDEX "notificaciones_tipo_idx" ON "notificaciones"("tipo");

-- CreateIndex
CREATE UNIQUE INDEX "configuracion_sistema_clave_key" ON "configuracion_sistema"("clave");

-- CreateIndex
CREATE INDEX "configuracion_sistema_clave_idx" ON "configuracion_sistema"("clave");

-- AddForeignKey
ALTER TABLE "usuarios" ADD CONSTRAINT "usuarios_activadoPor_fkey" FOREIGN KEY ("activadoPor") REFERENCES "usuarios"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "usuarios" ADD CONSTRAINT "usuarios_idDoctorAsignado_fkey" FOREIGN KEY ("idDoctorAsignado") REFERENCES "usuarios"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "registros_salud" ADD CONSTRAINT "registros_salud_usuarioId_fkey" FOREIGN KEY ("usuarioId") REFERENCES "usuarios"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "asignaciones_ejercicio" ADD CONSTRAINT "asignaciones_ejercicio_ejercicioId_fkey" FOREIGN KEY ("ejercicioId") REFERENCES "ejercicios"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "asignaciones_ejercicio" ADD CONSTRAINT "asignaciones_ejercicio_pacienteId_fkey" FOREIGN KEY ("pacienteId") REFERENCES "usuarios"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "ejercicios_realizados" ADD CONSTRAINT "ejercicios_realizados_usuarioId_fkey" FOREIGN KEY ("usuarioId") REFERENCES "usuarios"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "ejercicios_realizados" ADD CONSTRAINT "ejercicios_realizados_ejercicioId_fkey" FOREIGN KEY ("ejercicioId") REFERENCES "ejercicios"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "ejercicios_realizados" ADD CONSTRAINT "ejercicios_realizados_asignacionId_fkey" FOREIGN KEY ("asignacionId") REFERENCES "asignaciones_ejercicio"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "notificaciones" ADD CONSTRAINT "notificaciones_usuarioId_fkey" FOREIGN KEY ("usuarioId") REFERENCES "usuarios"("id") ON DELETE CASCADE ON UPDATE CASCADE;
