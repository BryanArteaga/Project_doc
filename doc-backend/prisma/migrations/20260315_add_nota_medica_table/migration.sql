-- CreateTable
CREATE TABLE "notas_medicas" (
    "id" SERIAL NOT NULL PRIMARY KEY,
    "pacienteId" INTEGER NOT NULL,
    "doctorId" INTEGER NOT NULL,
    "contenido" TEXT NOT NULL,
    "fecha" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "actualizadoEn" TIMESTAMP(3) NOT NULL,
    CONSTRAINT "notas_medicas_pacienteId_fkey" FOREIGN KEY ("pacienteId") REFERENCES "usuarios" ("id") ON DELETE CASCADE,
    CONSTRAINT "notas_medicas_doctorId_fkey" FOREIGN KEY ("doctorId") REFERENCES "doctores" ("id") ON DELETE CASCADE
);

-- CreateIndex
CREATE INDEX "notas_medicas_pacienteId_fecha_idx" ON "notas_medicas"("pacienteId", "fecha");

-- CreateIndex
CREATE INDEX "notas_medicas_doctorId_idx" ON "notas_medicas"("doctorId");
