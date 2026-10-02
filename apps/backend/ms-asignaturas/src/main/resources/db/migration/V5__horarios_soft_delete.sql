-- Soft delete de horarios: se mantiene el minimo de un horario activo por asignatura.
ALTER TABLE horarios ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
CREATE INDEX IF NOT EXISTS idx_horario_active ON horarios (active);
