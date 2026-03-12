import React, { useEffect, useState } from 'react';
import { Plus, Trash2, Edit2, Share2, Search, ChevronLeft, ChevronRight } from 'lucide-react';
import toast from 'react-hot-toast';
import Layout from '../components/common/Layout/Layout';
import Loading from '../components/common/Loading';
import PageTransition from '../components/common/PageTransition';
import { ejercicioService, type Ejercicio, type CreateEjercicioRequest, type Enfermedad, type Intensidad } from '../services/ejercicio.service';
import { asignacionService } from '../services/asignacion.service';
import './GestionEjercicios.css';

const ENFERMEDADES: Enfermedad[] = ['Hipertensión', 'Diabetes', 'Obesidad', 'Colesterol alto'];
const INTENSIDADES: Intensidad[] = ['Baja', 'Media', 'Alta'];
const ITEMS_PER_PAGE = 10;

interface FormData extends CreateEjercicioRequest {}

const GestionEjercicios: React.FC = () => {
  const [ejercicios, setEjercicios] = useState<Ejercicio[]>([]);
  const [filtrados, setFiltrados] = useState<Ejercicio[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterEnfermedad, setFilterEnfermedad] = useState<Enfermedad | ''>('');
  const [filterIntensidad, setFilterIntensidad] = useState<Intensidad | ''>('');
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);

  const [formData, setFormData] = useState<FormData>({
    nombre: '',
    descripcion: '',
    duracion: 300,
    intensidad: 'Media',
    series: 3,
    descansoEntreSeries: 30,
    enfermedad: 'Hipertensión'
  });

  const [pacientes, setPacientes] = useState<any[]>([]);
  const [showAsignarModal, setShowAsignarModal] = useState(false);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [asignarData, setAsignarData] = useState({
    ejercicioId: 0,
    tipo: 'individual' as 'individual' | 'general',
    usuarioId: 0,
    enfermedad: ''
  });

  useEffect(() => {
    loadEjercicios(1);
    loadPacientes();
  }, []);

  useEffect(() => {
    aplicarFiltros();
  }, [ejercicios, searchTerm, filterEnfermedad, filterIntensidad]);

  const loadEjercicios = async (page: number = 1) => {
    try {
      setLoading(true);
      const datos = await ejercicioService.listarEjercicios({
        page,
        limit: ITEMS_PER_PAGE,
        enfermedad: filterEnfermedad || undefined,
        intensidad: filterIntensidad || undefined
      });
      setEjercicios(datos.ejercicios);
      setTotalPages(datos.paginacion.totalPaginas);
      setCurrentPage(page);
    } catch (error: any) {
      console.error('Error al cargar ejercicios:', error);
      toast.error('Error al cargar ejercicios');
    } finally {
      setLoading(false);
    }
  };

  const loadPacientes = async () => {
    try {
      const datos = await asignacionService.listarPacientes();
      setPacientes(datos);
    } catch (error) {
      console.error('Error al cargar pacientes');
    }
  };

  const aplicarFiltros = () => {
    let resultado = ejercicios;

    if (searchTerm) {
      resultado = resultado.filter(e =>
        e.nombre.toLowerCase().includes(searchTerm.toLowerCase()) ||
        e.descripcion?.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }

    setFiltrados(resultado);
  };

  const handleChangePage = (newPage: number) => {
    if (newPage < 1 || newPage > totalPages) return;
    loadEjercicios(newPage);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!formData.nombre.trim()) {
      toast.error('El nombre es requerido');
      return;
    }

    try {
      let dataToSend = { ...formData };

      // Eliminar el ID si existe (importante para nuevos ejercicios)
      delete (dataToSend as any).id;
      delete (dataToSend as any).createdAt;
      delete (dataToSend as any).updatedAt;

      // Si hay un archivo de imagen, convertirlo a base64
      if (imageFile) {
        const reader = new FileReader();
        const base64 = await new Promise<string>((resolve, reject) => {
          reader.onload = () => resolve(reader.result as string);
          reader.onerror = reject;
          reader.readAsDataURL(imageFile);
        });
        dataToSend.urlImagen = base64;
      } else if (!editingId) {
        // Para nuevos ejercicios sin imagen, enviar null
        dataToSend.urlImagen = undefined;
      }

      // No enviar urlVideo (se elimina de la interfaz)
      delete (dataToSend as any).urlVideo;

      if (editingId) {
        await ejercicioService.actualizarEjercicio(editingId, dataToSend);
        toast.success('Ejercicio actualizado');
      } else {
        await ejercicioService.crearEjercicio(dataToSend);
        toast.success('Ejercicio creado');
      }
      loadEjercicios(1);
      setShowForm(false);
      setEditingId(null);
      setImageFile(null);
      resetForm();
    } catch (error: any) {
      toast.error(error.response?.data?.error || 'Error al guardar ejercicio');
    }
  };

  const handleEdit = (ejercicio: Ejercicio) => {
    setFormData(ejercicio);
    setEditingId(ejercicio.id);
    setShowForm(true);
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('¿Eliminar este ejercicio?')) return;

    try {
      await ejercicioService.eliminarEjercicio(id);
      toast.success('Ejercicio eliminado');
      loadEjercicios(currentPage);
    } catch (error: any) {
      toast.error('Error al eliminar ejercicio');
    }
  };

  const handleAsignar = (ejercicio: Ejercicio) => {
    setAsignarData({
      ejercicioId: ejercicio.id,
      tipo: 'individual',
      usuarioId: 0,
      enfermedad: ejercicio.enfermedad
    });
    setShowAsignarModal(true);
  };

  const handleConfirmarAsignacion = async () => {
    try {
      if (asignarData.tipo === 'individual' && !asignarData.usuarioId) {
        toast.error('Selecciona un paciente');
        return;
      }

      const datos = {
        ejercicioId: asignarData.ejercicioId,
        usuarioId: asignarData.tipo === 'individual' ? asignarData.usuarioId : null,
        enfermedad: asignarData.tipo === 'general' ? asignarData.enfermedad : undefined
      };

      await asignacionService.crearAsignacion(datos as any);
      toast.success('Ejercicio asignado');
      setShowAsignarModal(false);
    } catch (error: any) {
      toast.error('Error al asignar ejercicio');
    }
  };

  const resetForm = () => {
    setFormData({
      nombre: '',
      descripcion: '',
      duracion: 300,
      intensidad: 'Media',
      series: 3,
      descansoEntreSeries: 30,
      enfermedad: 'Hipertensión'
    });
    setImageFile(null);
    setEditingId(null);
  };

  if (loading) return <Loading />;

  return (
    <PageTransition>
      <Layout>
        <div className="gestion-ejercicios">
          <div className="header">
            <h1>Gestión de Ejercicios</h1>
            <button 
              className="btn-primary" 
              onClick={() => {
                if (!showForm) {
                  // Si vamos a abrir el formulario, resetear los datos primero
                  resetForm();
                }
                setShowForm(!showForm);
              }}
            >
              <Plus size={20} /> Nuevo Ejercicio
            </button>
          </div>

          {/* FORMULARIO */}
          {showForm && (
            <div className="form-container">
              <h2>{editingId ? 'Editar' : 'Crear'} Ejercicio</h2>
              <form onSubmit={handleSubmit}>
                <div className="form-row">
                  <div className="form-group">
                    <label>Nombre *</label>
                    <input
                      type="text"
                      value={formData.nombre}
                      onChange={(e) => setFormData({ ...formData, nombre: e.target.value })}
                      placeholder="Ej: Caminata rápida"
                    />
                  </div>

                  <div className="form-group">
                    <label>Enfermedad *</label>
                    <select
                      value={formData.enfermedad}
                      onChange={(e) => setFormData({ ...formData, enfermedad: e.target.value as Enfermedad })}
                    >
                      {ENFERMEDADES.map(e => (
                        <option key={e} value={e}>{e}</option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="form-group full">
                  <label>Descripción</label>
                  <textarea
                    value={formData.descripcion || ''}
                    onChange={(e) => setFormData({ ...formData, descripcion: e.target.value })}
                    placeholder="Ej: Camina a paso ligero durante 10 minutos"
                    rows={3}
                  />
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label>Duración (segundos) *</label>
                    <input
                      type="number"
                      min={1}
                      value={formData.duracion}
                      onChange={(e) => setFormData({ ...formData, duracion: parseInt(e.target.value) })}
                    />
                  </div>

                  <div className="form-group">
                    <label>Intensidad *</label>
                    <select
                      value={formData.intensidad}
                      onChange={(e) => setFormData({ ...formData, intensidad: e.target.value as Intensidad })}
                    >
                      {INTENSIDADES.map(i => (
                        <option key={i} value={i}>{i}</option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label>Series *</label>
                    <input
                      type="number"
                      min={1}
                      value={formData.series}
                      onChange={(e) => setFormData({ ...formData, series: parseInt(e.target.value) })}
                    />
                  </div>

                  <div className="form-group">
                    <label>Descanso entre series (seg.) *</label>
                    <input
                      type="number"
                      min={0}
                      value={formData.descansoEntreSeries}
                      onChange={(e) => setFormData({ ...formData, descansoEntreSeries: parseInt(e.target.value) })}
                    />
                  </div>
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label>Imagen del Ejercicio</label>
                    <input
                      type="file"
                      accept="image/*"
                      onChange={(e) => {
                        const file = e.target.files?.[0];
                        if (file) {
                          if (file.size > 5 * 1024 * 1024) {
                            toast.error('La imagen no debe superar 5MB');
                            return;
                          }
                          setImageFile(file);
                        }
                      }}
                      className="file-input"
                    />
                    <small>Máx: 5MB (JPG, PNG, WebP)</small>
                  </div>

                  {(imageFile || formData.urlImagen) && (
                    <div className="form-group">
                      <label>Vista Previa</label>
                      <img
                        src={imageFile ? URL.createObjectURL(imageFile) : formData.urlImagen}
                        alt="Preview"
                        className="image-preview"
                      />
                    </div>
                  )}
                </div>

                <div className="form-actions">
                  <button type="submit" className="btn-primary">
                    {editingId ? 'Actualizar' : 'Crear'} Ejercicio
                  </button>
                  <button
                    type="button"
                    className="btn-secondary"
                    onClick={() => {
                      setShowForm(false);
                      resetForm();
                    }}
                  >
                    Cancelar
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* FILTROS Y BÚSQUEDA */}
          <div className="filtros">
            <div className="search-box">
              <Search size={20} />
              <input
                type="text"
                placeholder="Buscar ejercicio..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>

            <select
              value={filterEnfermedad}
              onChange={(e) => {
                setFilterEnfermedad(e.target.value as Enfermedad | '');
                setCurrentPage(1);
                loadEjercicios(1);
              }}
              className="filter-select"
            >
              <option value="">Todas las enfermedades</option>
              {ENFERMEDADES.map(e => (
                <option key={e} value={e}>{e}</option>
              ))}
            </select>

            <select
              value={filterIntensidad}
              onChange={(e) => {
                setFilterIntensidad(e.target.value as Intensidad | '');
                setCurrentPage(1);
                loadEjercicios(1);
              }}
              className="filter-select"
            >
              <option value="">Todas las intensidades</option>
              {INTENSIDADES.map(i => (
                <option key={i} value={i}>{i}</option>
              ))}
            </select>
          </div>

          {/* LISTA DE EJERCICIOS */}
          <div className="ejercicios-grid">
            {filtrados.length === 0 ? (
              <p className="no-results">No hay ejercicios disponibles</p>
            ) : (
              filtrados.map(ejercicio => (
                <div key={ejercicio.id} className="ejercicio-card">
                  <div className="card-header">
                    <div>
                      <h3>{ejercicio.nombre}</h3>
                      <p className="enfermedad">{ejercicio.enfermedad}</p>
                    </div>
                    <span className={`intensidad ${ejercicio.intensidad.toLowerCase()}`}>
                      {ejercicio.intensidad}
                    </span>
                  </div>

                  {ejercicio.descripcion && (
                    <p className="descripcion">{ejercicio.descripcion}</p>
                  )}

                  <div className="card-details">
                    <div className="detail">
                      <span className="label">Duración:</span>
                      <span>{(ejercicio.duracion / 60).toFixed(0)} min</span>
                    </div>
                    <div className="detail">
                      <span className="label">Series:</span>
                      <span>{ejercicio.series}</span>
                    </div>
                    <div className="detail">
                      <span className="label">Descanso:</span>
                      <span>{ejercicio.descansoEntreSeries}s</span>
                    </div>
                  </div>

                  <div className="card-actions">
                    <button
                      className="btn-action btn-assign"
                      onClick={() => handleAsignar(ejercicio)}
                      title="Asignar ejercicio"
                    >
                      <Share2 size={18} /> Asignar
                    </button>
                    <button
                      className="btn-action btn-edit"
                      onClick={() => handleEdit(ejercicio)}
                      title="Editar"
                    >
                      <Edit2 size={18} />
                    </button>
                    <button
                      className="btn-action btn-delete"
                      onClick={() => handleDelete(ejercicio.id)}
                      title="Eliminar"
                    >
                      <Trash2 size={18} />
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>

          {/* PAGINACIÓN */}
          {totalPages > 1 && (
            <div className="pagination">
              <button
                className="btn-pagination"
                onClick={() => handleChangePage(currentPage - 1)}
                disabled={currentPage === 1}
              >
                <ChevronLeft size={20} /> Anterior
              </button>

              <div className="page-info">
                Página <strong>{currentPage}</strong> de <strong>{totalPages}</strong>
              </div>

              <button
                className="btn-pagination"
                onClick={() => handleChangePage(currentPage + 1)}
                disabled={currentPage === totalPages}
              >
                Siguiente <ChevronRight size={20} />
              </button>
            </div>
          )}

          {/* MODAL ASIGNACIÓN */}
          {showAsignarModal && (
            <div className="modal-overlay">
              <div className="modal-content">
                <h2>Asignar Ejercicio</h2>

                <div className="form-group">
                  <label>Tipo de Asignación</label>
                  <div className="radio-group">
                    <label>
                      <input
                        type="radio"
                        checked={asignarData.tipo === 'individual'}
                        onChange={() => setAsignarData({ ...asignarData, tipo: 'individual' })}
                      />
                      Individual (a un paciente)
                    </label>
                    <label>
                      <input
                        type="radio"
                        checked={asignarData.tipo === 'general'}
                        onChange={() => setAsignarData({ ...asignarData, tipo: 'general' })}
                      />
                      General (por enfermedad)
                    </label>
                  </div>
                </div>

                {asignarData.tipo === 'individual' ? (
                  <div className="form-group">
                    <label>Paciente *</label>
                    <select
                      value={asignarData.usuarioId}
                      onChange={(e) => setAsignarData({ ...asignarData, usuarioId: parseInt(e.target.value) })}
                    >
                      <option value={0}>Seleccionar paciente...</option>
                      {pacientes.map(p => (
                        <option key={p.id} value={p.id}>
                          {p.nombre} {p.apellido} - {p.cedula}
                        </option>
                      ))}
                    </select>
                  </div>
                ) : (
                  <div className="form-group">
                    <label>Enfermedad</label>
                    <input
                      type="text"
                      value={asignarData.enfermedad}
                      disabled
                      className="disabled-input"
                    />
                    <small>Se asignará a todos los pacientes con esta enfermedad</small>
                  </div>
                )}

                <div className="modal-actions">
                  <button className="btn-primary" onClick={handleConfirmarAsignacion}>
                    Asignar
                  </button>
                  <button className="btn-secondary" onClick={() => setShowAsignarModal(false)}>
                    Cancelar
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </Layout>
    </PageTransition>
  );
};

export default GestionEjercicios;
