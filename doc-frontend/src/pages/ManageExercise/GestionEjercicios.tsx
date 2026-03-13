import React, { useEffect, useState } from 'react';
import { Plus, Trash2, Edit2, Search, ChevronLeft, ChevronRight, Users, X, Check, ArrowLeft } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import Layout from '../../components/common/Layout/Layout';
import Loading from '../../components/common/Loading';
import PageTransition from '../../components/common/PageTransition';
import { ejercicioService, type Ejercicio, type CreateEjercicioRequest, type Enfermedad, type Intensidad } from '../../services/ejercicio.service';
import { asignacionService } from '../../services/asignacion.service';
import { uploadService } from '../../services/upload.service';
import './GestionEjercicios.css';

const ENFERMEDADES: Enfermedad[] = ['Hipertensión', 'Diabetes', 'Obesidad', 'Colesterol alto'];
const INTENSIDADES: Intensidad[] = ['Baja', 'Media', 'Alta'];
const ITEMS_PER_PAGE = 12;

interface FormData extends CreateEjercicioRequest {
  duracionMinutos: number;
  duracionSegundos: number;
}

interface PacienteAsignado {
  id: number;
  nombre: string;
  apellido: string;
  enfermedad: string;
}

interface EjercicioConAsignaciones extends Ejercicio {
  pacientesAsignados?: PacienteAsignado[];
  totalAsignados?: number;
}

const GestionEjercicios: React.FC = () => {
  const navigate = useNavigate();
  const [ejercicios, setEjercicios] = useState<EjercicioConAsignaciones[]>([]);
  const [filtrados, setFiltrados] = useState<EjercicioConAsignaciones[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
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
    duracionMinutos: 5,
    duracionSegundos: 0,
    intensidad: 'Media',
    series: 3,
    descansoEntreSeries: 30,
    enfermedad: 'Hipertensión'
  });

  const [pacientes, setPacientes] = useState<any[]>([]);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [isDarkMode, setIsDarkMode] = useState(false);

  // Estados para validación y confirmación
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [showConfirmSave, setShowConfirmSave] = useState(false);
  const [showConfirmCancel, setShowConfirmCancel] = useState(false);
  const [originalFormData, setOriginalFormData] = useState<FormData | null>(null);
  const [filtrosAbiertos, setFiltrosAbiertos] = useState(false);
  const [selectedEjercicioDetalle, setSelectedEjercicioDetalle] = useState<EjercicioConAsignaciones | null>(null);

  // Detectar modo oscuro
  useEffect(() => {
    const checkDarkMode = () => {
      setIsDarkMode(document.documentElement.classList.contains('dark'));
    };
    checkDarkMode();

    const observer = new MutationObserver(checkDarkMode);
    observer.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ['class']
    });

    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    loadEjercicios(1);
    loadPacientes();
  }, []);

  useEffect(() => {
    aplicarFiltros();
  }, [ejercicios]);



  // Función para convertir mm:ss a segundos  
  const timeToSeconds = (minutos: number, segundos: number): number => {
    return (minutos * 60) + segundos;
  };

  // Función para convertir segundos a mm:ss
  const secondsToTime = (totalSeconds: number): { minutos: number; segundos: number } => {
    const minutos = Math.floor(totalSeconds / 60);
    const segundos = totalSeconds % 60;
    return { minutos, segundos };
  };

  const loadEjercicios = async (page: number = 1) => {
    try {
      setLoading(true);
      // Si hay búsqueda activa, cargar sin paginación (todo con límite alto)
      const limit = searchTerm ? 9999 : ITEMS_PER_PAGE;
      const actualPage = searchTerm ? 1 : page;

      const datos = await ejercicioService.listarEjercicios({
        page: actualPage,
        limit,
        enfermedad: filterEnfermedad || undefined,
        intensidad: filterIntensidad || undefined
      });

      // No cargar asignaciones por ahora (endpoint 404)
      const ejerciciosConAsignaciones = datos.ejercicios.map((ejercicio) => ({
        ...ejercicio,
        pacientesAsignados: [],
        totalAsignados: 0
      }));

      setEjercicios(ejerciciosConAsignaciones);
      setTotalPages(searchTerm ? 1 : datos.paginacion.totalPaginas);
      setCurrentPage(actualPage);
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

  const limpiarFiltros = async () => {
    try {
      setLoading(true);
      setFilterEnfermedad('');
      setFilterIntensidad('');
      setSearchTerm('');
      setCurrentPage(1);

      // Cargar sin filtros
      const datos = await ejercicioService.listarEjercicios({
        page: 1,
        limit: ITEMS_PER_PAGE,
      });

      const ejerciciosConAsignaciones = datos.ejercicios.map((ejercicio) => ({
        ...ejercicio,
        pacientesAsignados: [],
        totalAsignados: 0
      }));

      setEjercicios(ejerciciosConAsignaciones);
      setTotalPages(datos.paginacion.totalPaginas);
      setCurrentPage(1);
      toast.success('Filtros limpios');
    } catch (error: any) {
      console.error('Error al limpiar filtros:', error);
      toast.error('Error al limpiar filtros');
    } finally {
      setLoading(false);
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

    if (filterEnfermedad) {
      resultado = resultado.filter(e => e.enfermedad === filterEnfermedad);
    }

    if (filterIntensidad) {
      resultado = resultado.filter(e => e.intensidad === filterIntensidad);
    }

    setFiltrados(resultado);
  };

  const handleChangePage = (newPage: number) => {
    if (newPage < 1 || newPage > totalPages) return;
    loadEjercicios(newPage);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    // Si estamos editando, mostrar confirmación
    if (editingId) {
      setShowConfirmSave(true);
      return;
    }

    // Si estamos creando, guardar directamente
    await guardarEjercicio();
  };

  // Función para guardar ejercicio (llamada desde handleSubmit o confirmación)
  const guardarEjercicio = async () => {
    if (!validateForm()) {
      return;
    }

    try {
      // Convertir duración de mm:ss a segundos
      const duracionTotal = timeToSeconds(formData.duracionMinutos, formData.duracionSegundos);

      let dataToSend = {
        ...formData,
        duracion: duracionTotal
      };

      // Limpiar campos que no se envían
      delete (dataToSend as any).id;
      delete (dataToSend as any).createdAt;
      delete (dataToSend as any).updatedAt;
      delete (dataToSend as any).duracionMinutos;
      delete (dataToSend as any).duracionSegundos;

      // Manejo de imagen
      if (imageFile) {
        try {
          console.log('📤 Subiendo imagen:', imageFile.name);
          toast.loading('Subiendo imagen...');

          const uploadResponse = await uploadService.uploadEjercicioImagen(imageFile);
          dataToSend.urlImagen = uploadResponse.imageUrl;

          toast.dismiss();
          console.log('✅ Imagen subida:', uploadResponse.imageUrl);
        } catch (error: any) {
          toast.dismiss();
          console.error('❌ Error al subir imagen:', error);
          toast.error('Error al subir la imagen. Intente de nuevo');
          return;
        }
      } else if (!editingId) {
        dataToSend.urlImagen = undefined;
      }

      delete (dataToSend as any).urlVideo;

      if (editingId) {
        await ejercicioService.actualizarEjercicio(editingId, dataToSend);
        toast.success('Ejercicio actualizado');
      } else {
        await ejercicioService.crearEjercicio(dataToSend);
        toast.success('Ejercicio creado');
      }

      loadEjercicios(1);
      setShowModal(false);
      setShowConfirmSave(false);
      setEditingId(null);
      setImageFile(null);
      resetForm();
    } catch (error: any) {
      toast.error(error.response?.data?.error || 'Error al guardar ejercicio');
    }
  };

  const handleEdit = (ejercicio: EjercicioConAsignaciones) => {
    const { minutos, segundos } = secondsToTime(ejercicio.duracion);
    const editData: FormData = {
      ...ejercicio,
      duracionMinutos: minutos,
      duracionSegundos: segundos
    };
    setFormData(editData);
    setOriginalFormData(editData);
    setEditingId(ejercicio.id);
    setShowModal(true);
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

  const abrirModalDetalles = (ejercicio: EjercicioConAsignaciones) => {
    setSelectedEjercicioDetalle(ejercicio);
  };

  const cerrarModalDetalles = () => {
    setSelectedEjercicioDetalle(null);
  };

  const handleAsignarPaciente = async (ejercicioId: number, pacienteId: number) => {
    try {
      await asignacionService.crearAsignacion({
        ejercicioId,
        usuarioId: pacienteId
      });
      toast.success('Paciente asignado al ejercicio');
      loadEjercicios(currentPage);
    } catch (error: any) {
      toast.error('Error al asignar paciente');
    }
  };

  const handleDesasignarPaciente = async (ejercicioId: number, pacienteId: number) => {
    try {
      await asignacionService.eliminarAsignacionPaciente(ejercicioId, pacienteId);
      toast.success('Paciente desasignado del ejercicio');
      loadEjercicios(currentPage);
    } catch (error: any) {
      toast.error('Error al desasignar paciente');
    }
  };

  const resetForm = () => {
    setFormData({
      nombre: '',
      descripcion: '',
      duracion: 300,
      duracionMinutos: 5,
      duracionSegundos: 0,
      intensidad: 'Media',
      series: 3,
      descansoEntreSeries: 30,
      enfermedad: 'Hipertensión'
    });
    setImageFile(null);
    setEditingId(null);
    setFormErrors({});
    setOriginalFormData(null);
  };

  // Validar formulario
  const validateForm = (): boolean => {
    const errors: Record<string, string> = {};

    if (!formData.nombre.trim()) {
      errors.nombre = 'El nombre es requerido';
    }

    if (formData.duracionMinutos === null || formData.duracionMinutos === undefined || (formData.duracionMinutos === 0 && formData.duracionSegundos === 0)) {
      errors.duracion = 'La duración debe ser mayor a 0';
    }

    if (formData.series === null || formData.series === undefined || formData.series < 1) {
      errors.series = 'Las series deben ser mínimo 1';
    }

    if (formData.descansoEntreSeries === null || formData.descansoEntreSeries === undefined || formData.descansoEntreSeries < 0) {
      errors.descansoEntreSeries = 'El descanso no puede ser negativo';
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // Detectar si hay cambios
  const hasChanges = (): boolean => {
    if (!editingId || !originalFormData) return false;

    return JSON.stringify(formData) !== JSON.stringify(originalFormData);
  };

  // Detectar si hay cambios en la creación
  const hasFormChanges = (): boolean => {
    const emptyForm: FormData = {
      nombre: '',
      descripcion: '',
      duracion: 300,
      duracionMinutos: 5,
      duracionSegundos: 0,
      intensidad: 'Media',
      series: 3,
      descansoEntreSeries: 30,
      enfermedad: 'Hipertensión'
    };

    return formData.nombre.trim() !== '' ||
      formData.descripcion !== '' ||
      JSON.stringify(formData) !== JSON.stringify(emptyForm);
  };

  // Manejar cierre de modal con confirmación
  const handleCloseModal = () => {
    if (editingId && hasChanges()) {
      // Si estamos editando y hay cambios, pedir confirmación
      setShowConfirmCancel(true);
    } else if (!editingId && hasFormChanges()) {
      // Si estamos creando y hay cambios, pedir confirmación
      setShowConfirmCancel(true);
    } else {
      setShowModal(false);
      resetForm();
    }
  };

  // Confirmar cancelación
  const handleConfirmCancel = () => {
    setShowConfirmCancel(false);
    setShowModal(false);
    resetForm();
  };

  if (loading) return (
    <Layout>
      <Loading message="Cargando ejercicios..." fullHeight />
    </Layout>
  );

  const handleSearchKeyPress = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      setCurrentPage(1);
      loadEjercicios(1);
    }
  };

  const getEnfermedadClass = (enfermedad: Enfermedad) => {
    const map: Record<Enfermedad, string> = {
      'Hipertensión': 'hipertension',
      'Diabetes': 'diabetes',
      'Obesidad': 'obesidad',
      'Colesterol alto': 'colesterol'
    };
    return map[enfermedad] || '';
  };

  return (
    <Layout>
      <PageTransition>
        {/* Header - Fuera del contenedor principal */}
        <div className="gestion-ejercicios-header">
          <button
            onClick={() => navigate('/admin')}
            style={{
              backgroundColor: '#5B65DC',
              color: '#ffffff',
              border: 'none',
              padding: '0.5rem 1rem',
              borderRadius: '0.5rem',
              fontWeight: 600,
              cursor: 'pointer',
              fontSize: '1rem',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.5rem',
              transition: 'all 0.3s ease',
              boxShadow: '0 4px 20px rgba(91, 101, 220, 0.15)'
            }}
            onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#4A52C4'}
            onMouseLeave={(e) => e.currentTarget.style.backgroundColor = '#5B65DC'}
          >
            <ArrowLeft size={20} />
            Volver
          </button>
          <h1 className="gestion-ejercicios-title">Gestión de Ejercicios</h1>
          <button
            className="btn-primary"
            onClick={() => {
              resetForm();
              setShowModal(true);
            }}
          >
            <Plus size={20} /> Nuevo Ejercicio
          </button>
        </div>

        {/* Contenedor principal */}
        <div className={`gestion-ejercicios ${isDarkMode ? 'dark-mode' : ''} ${filtrosAbiertos ? 'filtros-abiertos' : 'filtros-ocultos'}`}>
          {/* Búsqueda */}
          <div className="search-section">
            <div className="search-bar">
              <Search size={20} />
              <input
                type="text"
                placeholder="Buscar ejercicios..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                onKeyPress={handleSearchKeyPress}
              />
            </div>
            <button className="btn-search" onClick={() => {
              setCurrentPage(1);
              loadEjercicios(1);
            }}>
              Buscar
            </button>
            <button
              className="btn-filtros-toggle"
              onClick={() => setFiltrosAbiertos(!filtrosAbiertos)}
              title={filtrosAbiertos ? 'Ocultar filtros' : 'Mostrar filtros'}
            >
              {filtrosAbiertos ? 'Ocultar' : 'Filtros'}
            </button>

            {/* Filtros - Dentro de search-section para overlay correcto */}
            <div className={`filters-wrapper ${!filtrosAbiertos ? 'filtros-ocultos' : 'filtros-abiertos'}`}>
              <div className={`filtros-panel ${filtrosAbiertos ? 'abierto' : ''}`}>
                <div className="filtros-header">
                  <h3>Filtros</h3>
                  <button
                    className="btn-close-filtros"
                    onClick={() => setFiltrosAbiertos(false)}
                  >
                    <X size={20} />
                  </button>
                </div>

                <div className="filtros-content">
                  <div className="filtros-group">
                    <label>Enfermedad</label>
                    <select
                      value={filterEnfermedad}
                      onChange={(e) => setFilterEnfermedad(e.target.value as Enfermedad | '')}
                    >
                      <option value="">Todas las enfermedades</option>
                      {ENFERMEDADES.map(e => (
                        <option key={e} value={e}>{e}</option>
                      ))}
                    </select>
                  </div>

                  <div className="filtros-group">
                    <label>Intensidad</label>
                    <select
                      value={filterIntensidad}
                      onChange={(e) => setFilterIntensidad(e.target.value as Intensidad | '')}
                    >
                      <option value="">Todas las intensidades</option>
                      {INTENSIDADES.map(i => (
                        <option key={i} value={i}>{i}</option>
                      ))}
                    </select>
                  </div>

                  <button
                    className="btn-aplicar-filtros"
                    onClick={() => {
                      setCurrentPage(1);
                      loadEjercicios(1);
                      setFiltrosAbiertos(false);
                    }}
                  >
                    Aplicar Filtros
                  </button>

                  {(filterEnfermedad || filterIntensidad) && (
                    <button
                      className="btn-limpiar-filtros"
                      onClick={() => limpiarFiltros()}
                    >
                      Limpiar Filtros
                    </button>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Lista de ejercicios */}
          <div className="ejercicios-container">
            <div className="ejercicios-grid">
              {filtrados.map((ejercicio) => (
                <div
                  key={ejercicio.id}
                  className="ejercicio-card"
                  onClick={() => abrirModalDetalles(ejercicio)}
                  style={{ cursor: 'pointer' }}
                >
                  <div className="ejercicio-header">
                    <h3>{ejercicio.nombre}</h3>
                    <div className="ejercicio-actions" onClick={(e) => e.stopPropagation()}>
                      <button
                        className="btn-icon"
                        onClick={() => handleEdit(ejercicio)}
                        title="Editar"
                      >
                        <Edit2 size={18} />
                      </button>
                      <button
                        className="btn-icon delete"
                        onClick={() => handleDelete(ejercicio.id)}
                        title="Eliminar"
                      >
                        <Trash2 size={18} />
                      </button>
                    </div>
                  </div>

                  <div className="ejercicio-info">
                    <p className="descripcion">{ejercicio.descripcion}</p>
                    <div className="ejercicio-details">
                      <span className={`intensidad ${ejercicio.intensidad.toLowerCase()}`}>
                        {ejercicio.intensidad}
                      </span>
                      <span
                        className={`enfermedad ${getEnfermedadClass(ejercicio.enfermedad)}`}
                      >
                        {ejercicio.enfermedad}
                      </span>
                      <span className="duracion">
                        {Math.floor(ejercicio.duracion / 60)}:{(ejercicio.duracion % 60).toString().padStart(2, '0')}
                      </span>
                    </div>
                    <div className="asignados-badge">
                      <Users size={14} />
                      <span>{ejercicio.totalAsignados || 0} pacientes</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {/* Paginación */}
            {totalPages > 1 && (
              <div className="pagination">
                {/* Botón anterior */}
                <button
                  className="pagination-btn pagination-arrow"
                  onClick={() => handleChangePage(currentPage - 1)}
                  disabled={currentPage === 1}
                  title="Página anterior"
                >
                  <ChevronLeft size={18} />
                </button>

                {/* Números de página */}
                <div className="pagination-numbers">
                  {(() => {
                    const pageRange = 4;
                    let startPage, endPage;

                    if (currentPage <= 3) {
                      // Primeras 3 páginas: mostrar [1, 2, 3, 4]
                      startPage = 1;
                      endPage = Math.min(pageRange, totalPages);
                    } else {
                      // Después: mostrar los últimos 4 o desde currentPage si está en los últimos
                      if (currentPage >= totalPages - pageRange + 1) {
                        // Estamos en los últimos 4, mostrar los últimos 4
                        startPage = Math.max(1, totalPages - pageRange + 1);
                        endPage = totalPages;
                      } else {
                        // En el medio: mostrar desde currentPage
                        startPage = currentPage;
                        endPage = Math.min(totalPages, currentPage + pageRange - 1);
                      }
                    }

                    return Array.from({ length: endPage - startPage + 1 }, (_, i) => startPage + i)
                      .map((page) => (
                        <button
                          key={page}
                          className={`pagination-number ${currentPage === page ? 'active' : ''}`}
                          onClick={() => handleChangePage(page)}
                        >
                          {page}
                        </button>
                      ));
                  })()}
                </div>

                {/* Botón siguiente */}
                <button
                  className="pagination-btn pagination-arrow"
                  onClick={() => handleChangePage(currentPage + 1)}
                  disabled={currentPage === totalPages}
                  title="Página siguiente"
                >
                  <ChevronRight size={18} />
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Modales - Dentro del contenedor principal pero fuera del ejercicios-container */}
        {/* Modal del formulario */}
        {showModal && (
          <div className="modal-overlay">
            <div className="modal-container">
              <div className="modal-header">
                <h2>{editingId ? 'Editar' : 'Crear'} Ejercicio</h2>
                <button
                  type="button"
                  className="modal-close"
                  onClick={handleCloseModal}
                >
                  <X size={24} />
                </button>
              </div>

              <form onSubmit={handleSubmit} className="modal-form">
                <div className="form-row">
                  <div className="form-group">
                    <label>Nombre *</label>
                    <input
                      type="text"
                      value={formData.nombre}
                      onChange={(e) => setFormData({ ...formData, nombre: e.target.value })}
                      placeholder="Ej: Caminata rápida"
                      className={formErrors.nombre ? 'input-error' : ''}
                    />
                    {formErrors.nombre && <span className="error-message">{formErrors.nombre}</span>}
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
                    <label>Duración *</label>
                    <div className="time-input">
                      <input
                        type="number"
                        min={0}
                        max={59}
                        value={formData.duracionMinutos === 0 && formData.duracionSegundos === 0 ? '' : formData.duracionMinutos}
                        onChange={(e) => setFormData({
                          ...formData,
                          duracionMinutos: e.target.value === '' ? 0 : parseInt(e.target.value) || 0
                        })}
                        placeholder="MM"
                        className={formErrors.duracion ? 'input-error' : ''}
                      />
                      <span>:</span>
                      <input
                        type="number"
                        min={0}
                        max={59}
                        value={formData.duracionMinutos === 0 && formData.duracionSegundos === 0 ? '' : formData.duracionSegundos}
                        onChange={(e) => setFormData({
                          ...formData,
                          duracionSegundos: e.target.value === '' ? 0 : parseInt(e.target.value) || 0
                        })}
                        placeholder="SS"
                        className={formErrors.duracion ? 'input-error' : ''}
                      />
                    </div>
                    {formErrors.duracion && <span className="error-message">{formErrors.duracion}</span>}
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
                      value={formData.series || ''}
                      onChange={(e) => setFormData({ ...formData, series: e.target.value === '' ? 0 : parseInt(e.target.value) || 0 })}
                      placeholder="Ej: 3"
                      className={formErrors.series ? 'input-error' : ''}
                    />
                    {formErrors.series && <span className="error-message">{formErrors.series}</span>}
                  </div>

                  <div className="form-group">
                    <label>Descanso entre series (seg) *</label>
                    <input
                      type="number"
                      min={0}
                      value={formData.descansoEntreSeries || ''}
                      onChange={(e) => setFormData({ ...formData, descansoEntreSeries: e.target.value === '' ? 0 : parseInt(e.target.value) || 0 })}
                      placeholder="Ej: 30"
                      className={formErrors.descansoEntreSeries ? 'input-error' : ''}
                    />
                    {formErrors.descansoEntreSeries && <span className="error-message">{formErrors.descansoEntreSeries}</span>}
                  </div>
                </div>

                <div className="form-group">
                  <label>Imagen</label>
                  <input
                    type="file"
                    accept="image/*"
                    onChange={(e) => setImageFile(e.target.files?.[0] || null)}
                  />
                </div>

                <div className="modal-actions">
                  <button
                    type="button"
                    className="btn-secondary"
                    onClick={handleCloseModal}
                  >
                    Cancelar
                  </button>
                  <button type="submit" className="btn-primary">
                    {editingId ? 'Actualizar' : 'Crear'} Ejercicio
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Modal de confirmación para guardar */}
        {showConfirmSave && (
          <div className="modal-overlay">
            <div className="modal-container confirm-modal">
              <div className="modal-header">
                <h2>Confirmar cambios</h2>
              </div>
              <div className="modal-content">
                <p>¿Deseas guardar los cambios en este ejercicio?</p>
              </div>
              <div className="modal-actions">
                <button
                  className="btn-secondary"
                  onClick={() => setShowConfirmSave(false)}
                >
                  Mantener editando
                </button>
                <button
                  className="btn-primary"
                  onClick={guardarEjercicio}
                >
                  Guardar cambios
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Modal de confirmación para cancelar */}
        {showConfirmCancel && (
          <div className="modal-overlay">
            <div className="modal-container confirm-modal">
              <div className="modal-header">
                <h2>¿Descartar cambios?</h2>
              </div>
              <div className="modal-content">
                <p>Tienes cambios sin guardar. ¿Deseas descartarlos y cerrar?</p>
              </div>
              <div className="modal-actions">
                <button
                  className="btn-secondary"
                  onClick={() => setShowConfirmCancel(false)}
                >
                  Seguir editando
                </button>
                <button
                  className="btn-danger"
                  onClick={handleConfirmCancel}
                >
                  Descartar cambios
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Modal de detalles del ejercicio */}
        {selectedEjercicioDetalle && (
          <div className="modal-overlay" onClick={cerrarModalDetalles}>
            <div className="modal-container modal-detalles" onClick={(e) => e.stopPropagation()}>
              <div className="modal-header">
                <h2>{selectedEjercicioDetalle.nombre}</h2>
                <button
                  type="button"
                  className="modal-close"
                  onClick={cerrarModalDetalles}
                >
                  <X size={24} />
                </button>
              </div>

              <div className="modal-body detalles-body">
                {/* Información del ejercicio */}
                <div className="detalles-section">
                  <h3>Información del Ejercicio</h3>
                  <div className="detalles-content">
                    <div className="detalle-item">
                      <span className="label">Descripción:</span>
                      <p>{selectedEjercicioDetalle.descripcion}</p>
                    </div>
                    <div className="detalles-grid">
                      <div className="detalle-item">
                        <span className="label">Intensidad:</span>
                        <span className={`intensidad ${selectedEjercicioDetalle.intensidad.toLowerCase()}`}>
                          {selectedEjercicioDetalle.intensidad}
                        </span>
                      </div>
                      <div className="detalle-item">
                        <span className="label">Enfermedad:</span>
                        <span className={`enfermedad ${getEnfermedadClass(selectedEjercicioDetalle.enfermedad)}`}>
                          {selectedEjercicioDetalle.enfermedad}
                        </span>
                      </div>
                      <div className="detalle-item">
                        <span className="label">Duración:</span>
                        <span>{Math.floor(selectedEjercicioDetalle.duracion / 60)}:{(selectedEjercicioDetalle.duracion % 60).toString().padStart(2, '0')} min</span>
                      </div>
                      <div className="detalle-item">
                        <span className="label">Series:</span>
                        <span>{selectedEjercicioDetalle.series}</span>
                      </div>
                      <div className="detalle-item">
                        <span className="label">Descanso entre series:</span>
                        <span>{selectedEjercicioDetalle.descansoEntreSeries}s</span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Pacientes asignados */}
                <div className="detalles-section">
                  <div className="detalles-header">
                    <h3>Pacientes Asignados</h3>
                    <span className="badge">{selectedEjercicioDetalle.totalAsignados || 0}</span>
                  </div>

                  {selectedEjercicioDetalle.pacientesAsignados && selectedEjercicioDetalle.pacientesAsignados.length > 0 ? (
                    <div className="pacientes-modal-list">
                      {selectedEjercicioDetalle.pacientesAsignados.map((paciente) => (
                        <div key={paciente.id} className="paciente-modal-item">
                          <div className="paciente-modal-info">
                            <span className="nombre">{paciente.nombre} {paciente.apellido}</span>
                            <span className="enfermedad">{paciente.enfermedad}</span>
                          </div>
                          <button
                            className="btn-remove-modal"
                            onClick={() => handleDesasignarPaciente(selectedEjercicioDetalle.id, paciente.id)}
                            title="Desasignar"
                          >
                            <Trash2 size={16} />
                          </button>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="no-data">No hay pacientes asignados</p>
                  )}
                </div>

                {/* Asignar nuevos pacientes */}
                <div className="detalles-section">
                  <h3>Asignar Nuevos Pacientes</h3>
                  <div className="pacientes-modal-disponibles">
                    {pacientes.filter(p => !selectedEjercicioDetalle.pacientesAsignados?.some(pa => pa.id === p.id)).length > 0 ? (
                      pacientes
                        .filter(p => !selectedEjercicioDetalle.pacientesAsignados?.some(pa => pa.id === p.id))
                        .map((paciente) => (
                          <div key={paciente.id} className="paciente-disponible-modal">
                            <div className="paciente-info-disponible">
                              <span className="nombre">{paciente.nombre} {paciente.apellido}</span>
                              <span className="sub-info">{paciente.enfermedad}</span>
                            </div>
                            <button
                              className="btn-assign"
                              onClick={() => {
                                handleAsignarPaciente(selectedEjercicioDetalle.id, paciente.id);
                                // Recargar los detalles después de asignar
                                setTimeout(() => {
                                  const ejercicioActualizado = ejercicios.find(e => e.id === selectedEjercicioDetalle.id);
                                  if (ejercicioActualizado) {
                                    setSelectedEjercicioDetalle(ejercicioActualizado);
                                  }
                                }, 500);
                              }}
                              title="Asignar"
                            >
                              <Check size={16} />
                            </button>
                          </div>
                        ))
                    ) : (
                      <p className="no-data">Todos los pacientes están asignados</p>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </PageTransition>
    </Layout>
  );
};


export default GestionEjercicios;
