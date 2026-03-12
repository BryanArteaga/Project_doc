import React, { useEffect, useState } from 'react';
import { Plus, Trash2, Edit2, Check, X } from 'lucide-react';
import toast from 'react-hot-toast';
import Layout from '../../components/common/Layout/Layout';
import Loading from '../../components/common/Loading';
import PageTransition from '../../components/common/PageTransition';
import { asignacionService, type Asignacion, type Ejercicio } from '../../services/asignacion.service';
import './AsignacionesEjercicios.css';

interface FormData {
  ejercicioId: number;
  usuarioId?: number | null;
  enfermedad?: string;
  descripcion?: string;
  fechaExpiracion?: string;
  tipo: 'general' | 'individual'; // general = todospacientes, individual = un paciente
}

const AsignacionesEjercicios: React.FC = () => {
  const [asignaciones, setAsignaciones] = useState<Asignacion[]>([]);
  const [ejercicios, setEjercicios] = useState<Ejercicio[]>([]);
  const [pacientes, setPacientes] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [formData, setFormData] = useState<FormData>({
    ejercicioId: 0,
    tipo: 'general',
  });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [asignacionesData, ejerciciosData, pacientesData] = await Promise.all([
        asignacionService.listarAsignaciones(),
        asignacionService.listarEjercicios(),
        asignacionService.listarPacientes(),
      ]);
      setAsignaciones(asignacionesData.asignaciones);
      setEjercicios(ejerciciosData);
      setPacientes(pacientesData);
    } catch (error: any) {
      console.error('Error al cargar datos:', error);
      toast.error('Error al cargar los datos');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!formData.ejercicioId) {
      toast.error('Selecciona un ejercicio');
      return;
    }

    if (formData.tipo === 'individual' && !formData.usuarioId) {
      toast.error('Selecciona un paciente');
      return;
    }

    try {
      const submitData = {
        ejercicioId: formData.ejercicioId,
        usuarioId: formData.tipo === 'individual' ? formData.usuarioId : null,
        enfermedad: formData.tipo === 'general' ? formData.enfermedad : undefined,
        descripcion: formData.descripcion,
        fechaExpiracion: formData.fechaExpiracion,
      };

      if (editingId) {
        await asignacionService.actualizarAsignacion(editingId, submitData);
        toast.success('Asignación actualizada');
      } else {
        await asignacionService.crearAsignacion(submitData);
        toast.success('Asignación creada');
      }

      resetForm();
      await loadData();
    } catch (error: any) {
      console.error('Error:', error);
      toast.error(error.response?.data?.error || 'Error al guardar la asignación');
    }
  };

  const handleEdit = (asignacion: Asignacion) => {
    setFormData({
      ejercicioId: asignacion.ejercicioId,
      usuarioId: asignacion.usuarioId,
      enfermedad: asignacion.enfermedad,
      descripcion: asignacion.descripcion,
      fechaExpiracion: asignacion.fechaExpiracion,
      tipo: asignacion.usuarioId ? 'individual' : 'general',
    });
    setEditingId(asignacion.id);
    setShowForm(true);
  };

  const handleDelete = async (id: number) => {
    if (window.confirm('¿Eliminar esta asignación?')) {
      try {
        await asignacionService.eliminarAsignacion(id);
        toast.success('Asignación eliminada');
        await loadData();
      } catch (error: any) {
        console.error('Error:', error);
        toast.error('Error al eliminar la asignación');
      }
    }
  };

  const resetForm = () => {
    setShowForm(false);
    setEditingId(null);
    setFormData({
      ejercicioId: 0,
      tipo: 'general',
    });
  };

  if (loading) {
    return (
      <Layout>
        <Loading message="Cargando asignaciones..." fullHeight />
      </Layout>
    );
  }

  return (
    <Layout>
      <PageTransition>
        <div className="asignaciones-container">
          {/* Header */}
          <div className="asignaciones-header">
            <h1>Gestionar Asignaciones de Ejercicios</h1>
            <button
              onClick={() => setShowForm(!showForm)}
              className="btn-primary"
            >
              <Plus className="w-5 h-5" />
              {showForm ? 'Cancelar' : 'Nueva Asignación'}
            </button>
          </div>

          {/* Formulario */}
          {showForm && (
            <form onSubmit={handleSubmit} className="asignacion-form">
              <div className="form-grid">
                {/* Tipo de asignación */}
                <div className="form-group">
                  <label>Tipo de Asignación</label>
                  <div className="radio-group">
                    <label>
                      <input
                        type="radio"
                        value="general"
                        checked={formData.tipo === 'general'}
                        onChange={(e) =>
                          setFormData({ ...formData, tipo: e.target.value as 'general' | 'individual', usuarioId: undefined })
                        }
                      />
                      General (Todos los pacientes con enfermedad)
                    </label>
                    <label>
                      <input
                        type="radio"
                        value="individual"
                        checked={formData.tipo === 'individual'}
                        onChange={(e) =>
                          setFormData({ ...formData, tipo: e.target.value as 'general' | 'individual', enfermedad: undefined })
                        }
                      />
                      Individual (Paciente específico)
                    </label>
                  </div>
                </div>

                {/* Ejercicio */}
                <div className="form-group">
                  <label htmlFor="ejercicio">Ejercicio *</label>
                  <select
                    id="ejercicio"
                    value={formData.ejercicioId}
                    onChange={(e) => setFormData({ ...formData, ejercicioId: parseInt(e.target.value) })}
                    required
                  >
                    <option value={0}>Selecciona un ejercicio</option>
                    {ejercicios.map((ej) => (
                      <option key={ej.id} value={ej.id}>
                        {ej.nombre} ({ej.intensidad})
                      </option>
                    ))}
                  </select>
                </div>

                {/* Paciente (si es individual) */}
                {formData.tipo === 'individual' && (
                  <div className="form-group">
                    <label htmlFor="paciente">Paciente *</label>
                    <select
                      id="paciente"
                      value={formData.usuarioId || 0}
                      onChange={(e) => setFormData({ ...formData, usuarioId: parseInt(e.target.value) || undefined })}
                      required
                    >
                      <option value={0}>Selecciona un paciente</option>
                      {pacientes.map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.nombre} {p.apellido} - {p.enfermedad}
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                {/* Enfermedad (si es general) */}
                {formData.tipo === 'general' && (
                  <div className="form-group">
                    <label htmlFor="enfermedad">Enfermedad (opcional)</label>
                    <input
                      id="enfermedad"
                      type="text"
                      placeholder="ej: Hipertensión, Diabetes"
                      value={formData.enfermedad || ''}
                      onChange={(e) => setFormData({ ...formData, enfermedad: e.target.value })}
                    />
                  </div>
                )}

                {/* Descripción */}
                <div className="form-group full-width">
                  <label htmlFor="descripcion">Descripción</label>
                  <textarea
                    id="descripcion"
                    placeholder="Notas o instrucciones especiales"
                    value={formData.descripcion || ''}
                    onChange={(e) => setFormData({ ...formData, descripcion: e.target.value })}
                    rows={3}
                  />
                </div>

                {/* Fecha de expiración */}
                <div className="form-group">
                  <label htmlFor="expiracion">Fecha de Expiración</label>
                  <input
                    id="expiracion"
                    type="date"
                    value={formData.fechaExpiracion || ''}
                    onChange={(e) => setFormData({ ...formData, fechaExpiracion: e.target.value })}
                  />
                </div>
              </div>

              <div className="form-actions">
                <button type="submit" className="btn-primary">
                  <Check className="w-5 h-5" />
                  {editingId ? 'Actualizar' : 'Crear'} Asignación
                </button>
                <button
                  type="button"
                  onClick={resetForm}
                  className="btn-secondary"
                >
                  <X className="w-5 h-5" />
                  Cancelar
                </button>
              </div>
            </form>
          )}

          {/* Lista de asignaciones */}
          <div className="asignaciones-list">
            <h2>Asignaciones Actuales</h2>
            {asignaciones.length === 0 ? (
              <p className="empty-state">No hay asignaciones. ¡Crea una nueva!</p>
            ) : (
              <div className="list-container">
                {asignaciones.map((asignacion) => (
                  <div key={asignacion.id} className="asignacion-card">
                    <div className="card-header">
                      <div>
                        <h3>{asignacion.ejercicio?.nombre || 'Ejercicio'}</h3>
                        <span className={`badge ${asignacion.activo ? 'active' : 'inactive'}`}>
                          {asignacion.activo ? 'Activo' : 'Inactivo'}
                        </span>
                        {asignacion.usuarioId ? (
                          <span className="badge individual">Individual</span>
                        ) : (
                          <span className="badge general">General</span>
                        )}
                      </div>
                      <div className="card-actions">
                        <button
                          onClick={() => handleEdit(asignacion)}
                          className="btn-icon"
                          title="Editar"
                        >
                          <Edit2 className="w-5 h-5" />
                        </button>
                        <button
                          onClick={() => handleDelete(asignacion.id)}
                          className="btn-icon delete"
                          title="Eliminar"
                        >
                          <Trash2 className="w-5 h-5" />
                        </button>
                      </div>
                    </div>

                    <div className="card-body">
                      {asignacion.usuario && (
                        <p>
                          <strong>Paciente:</strong> {asignacion.usuario.nombre} {asignacion.usuario.apellido}
                        </p>
                      )}
                      {asignacion.enfermedad && (
                        <p>
                          <strong>Enfermedad:</strong> {asignacion.enfermedad}
                        </p>
                      )}
                      {asignacion.descripcion && (
                        <p>
                          <strong>Descripción:</strong> {asignacion.descripcion}
                        </p>
                      )}
                      <p>
                        <strong>Duración:</strong> {asignacion.ejercicio?.duracion}s x {asignacion.ejercicio?.series} series
                      </p>
                      <p>
                        <strong>Intensidad:</strong> {asignacion.ejercicio?.intensidad}
                      </p>
                      {asignacion.fechaExpiracion && (
                        <p>
                          <strong>Vence:</strong> {new Date(asignacion.fechaExpiracion).toLocaleDateString()}
                        </p>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </PageTransition>
    </Layout>
  );
};

export default AsignacionesEjercicios;
