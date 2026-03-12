-- CreateTable AsignacionEjercicio
CREATE TABLE "asignaciones_ejercicios" (
    "id" SERIAL NOT NULL,
    "ejercicioId" INTEGER NOT NULL,
    "doctorId" INTEGER NOT NULL,
    "usuarioId" INTEGER,
    "enfermedad" TEXT,
    "descripcion" TEXT,
    "activo" BOOLEAN NOT NULL DEFAULT true,
    "fechaAsignacion" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "fechaExpiracion" TIMESTAMP(3),
    "creadoEn" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "actualizadoEn" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "asignaciones_ejercicios_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "asignaciones_ejercicios_ejercicioId_doctorId_usuarioId_key" ON "asignaciones_ejercicios"("ejercicioId", "doctorId", "usuarioId");

-- CreateIndex
CREATE INDEX "asignaciones_ejercicios_doctorId_idx" ON "asignaciones_ejercicios"("doctorId");

-- CreateIndex
CREATE INDEX "asignaciones_ejercicios_usuarioId_idx" ON "asignaciones_ejercicios"("usuarioId");

-- CreateIndex
CREATE INDEX "asignaciones_ejercicios_enfermedad_idx" ON "asignaciones_ejercicios"("enfermedad");

-- CreateIndex
CREATE INDEX "asignaciones_ejercicios_activo_idx" ON "asignaciones_ejercicios"("activo");

-- AddForeignKey
ALTER TABLE "asignaciones_ejercicios" ADD CONSTRAINT "asignaciones_ejercicios_ejercicioId_fkey" FOREIGN KEY ("ejercicioId") REFERENCES "ejercicios"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "asignaciones_ejercicios" ADD CONSTRAINT "asignaciones_ejercicios_doctorId_fkey" FOREIGN KEY ("doctorId") REFERENCES "doctores"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "asignaciones_ejercicios" ADD CONSTRAINT "asignaciones_ejercicios_usuarioId_fkey" FOREIGN KEY ("usuarioId") REFERENCES "usuarios"("id") ON DELETE CASCADE ON UPDATE CASCADE;
