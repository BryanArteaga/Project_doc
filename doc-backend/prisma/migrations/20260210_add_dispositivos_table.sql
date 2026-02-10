-- CreateTable
CREATE TABLE "dispositivos" (
    "id" SERIAL NOT NULL,
    "usuarioId" INTEGER NOT NULL,
    "fcmToken" TEXT NOT NULL,
    "deviceId" TEXT,
    "activo" BOOLEAN NOT NULL DEFAULT true,
    "ultimaVezVisto" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "creadoEn" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "actualizadoEn" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "dispositivos_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "dispositivos_fcmToken_key" ON "dispositivos"("fcmToken");

-- CreateIndex
CREATE INDEX "dispositivos_usuarioId_idx" ON "dispositivos"("usuarioId");

-- CreateIndex
CREATE INDEX "dispositivos_fcmToken_idx" ON "dispositivos"("fcmToken");

-- AddForeignKey
ALTER TABLE "dispositivos" ADD CONSTRAINT "dispositivos_usuarioId_fkey" FOREIGN KEY ("usuarioId") REFERENCES "usuarios"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- Update NotificationType enum to include new values
ALTER TYPE "NotificationType" ADD VALUE IF NOT EXISTS 'cuenta_activada';
ALTER TYPE "NotificationType" ADD VALUE IF NOT EXISTS 'health_reminder';
