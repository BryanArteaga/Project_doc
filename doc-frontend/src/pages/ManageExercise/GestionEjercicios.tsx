import React, { useEffect, useState } from 'react';
import { Plus, ArrowLeft } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import Layout from '../../components/common/Layout/Layout';
import Loading from '../../components/common/Loading';
import PageTransition from '../../components/common/PageTransition';
import { CRUDModal, ConfirmModal, SearchFilter, DataGrid, Pagination } from '../../components/shared';
import { ejercicioService, type Ejercicio, type CreateEjercicioRequest, type Enfermedad, type Intensidad } from '../../services/ejercicio.service';
import { asignacionService } from '../../services/asignacion.service';
import ExerciseDetailsModal from './ExerciseDetailsModal';
import './GestionEjercicios.css';

const ENFERMEDADES: Enfermedad[] = ['Hipertensión', 'Diabetes', 'Obesidad', 'Colesterol alto'];
const INTENSIDADES: Intensidad[] = ['Baja', 'Media', 'Alta'];
const ITEMS_PER_PAGE = 12;

interface FormData extends CreateEjercicioRequest {
  duracionMinutos: number | '';
  duracionSegundos: number | '';
  series: number | '';
  descansoEntreSeries: number | '';
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
  
  // Estados principales
  const [ejercicios, setEjercicios] = useState<EjercicioConAsignaciones[]>([]);
  const [filtrados, setFiltrados] = useState<EjercicioConAsignaciones[]>([]);
  const [loading, setLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isDarkMode, setIsDarkMode] = useState(false);

  // Modal states
  const [showModal, setShowModal] = useState(false);
  const [showConfirmSave, setShowConfirmSave] = useState(false);
  const [showConfirmCancel, setShowConfirmCancel] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [selectedEjercicioDetalle, setSelectedEjercicioDetalle] = useState<EjercicioConAsignaciones | null>(null);

  // Search & Filter states - Live input (being edited)
  const [searchInput, setSearchInput] = useState('');
  const [filterEnfermedad_temp, setFilterEnfermedad_temp] = useState<Enfermedad | ''>('');
  const [filterIntensidad_temp, setFilterIntensidad_temp] = useState<Intensidad | ''>('');
  
  // Search & Filter states - Applied (used in loadEjercicios)
  const [searchTerm, setSearchTerm] = useState('');
  const [filterEnfermedad, setFilterEnfermedad] = useState<Enfermedad | ''>('');
  const [filterIntensidad, setFilterIntensidad] = useState<Intensidad | ''>('');

  // Form states
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

  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [originalFormData, setOriginalFormData] = useState<FormData | null>(null);
  const [imageFile, setImageFile] = useState<File | null>(null);

  // Duplicate validation states
  const [showDuplicateWarning, setShowDuplicateWarning] = useState(false);
  const [duplicateInfo, setDuplicateInfo] = useState<{ ejercicio: string; enfermedad: Enfermedad } | null>(null);
  const [proceedWithDuplicate, setProceedWithDuplicate] = useState(false);

  // Pagination & Data
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [pacientes, setPacientes] = useState<any[]>([]);

  // Dark mode detection
  useEffect(() => {
    const checkDarkMode = () => setIsDarkMode(document.documentElement.classList.contains('dark'));
    checkDarkMode();
    const observer = new MutationObserver(checkDarkMode);
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] });
    return () => observer.disconnect();
  }, []);

  // Initial load
  useEffect(() => {
    loadEjercicios(1);
    loadPacientes();
  }, []);

  useEffect(() => {
    aplicarFiltros();
  }, [ejercicios, searchTerm, filterEnfermedad, filterIntensidad]);

  // ============ DATA LOADING ============
  const loadEjercicios = async (page: number = 1, search: string = searchTerm, enf: Enfermedad | '' = filterEnfermedad, intensidad: Intensidad | '' = filterIntensidad) => {
    try {
      setLoading(true);
      const limit = search ? 9999 : ITEMS_PER_PAGE;
      const actualPage = search ? 1 : page;

      const datos = await ejercicioService.listarEjercicios({
        page: actualPage,
        limit,
        enfermedad: enf || undefined,
        intensidad: intensidad || undefined
      });

      setEjercicios(datos.ejercicios);
      setTotalPages(search ? 1 : datos.paginacion.totalPaginas);
      setCurrentPage(actualPage);
    } catch (error: any) {
      console.error('Error cargando ejercicios:', error);
      toast.error('Error al cargar ejercicios');
    } finally {
      setLoading(false);
    }
  };

  const loadPacientes = async () => {
    try {
      const data = await asignacionService.listarPacientes();
      setPacientes(data || []);
    } catch (error: any) {
      console.error('Error cargando pacientes:', error);
    }
  };

  // ============ HELPER FUNCTIONS ============
  const getEnfermedadColor = (enfermedad?: string): string => {
    switch (enfermedad) {
      case 'Hipertensión': return '#B32428';
      case 'Diabetes': return '#E5BE01';
      case 'Obesidad': return '#2595AB';
      case 'Colesterol alto': return '#FF8000';
      default: return '#6b7280';
    }
  };

  // ============ FILTERING ============
  const aplicarFiltros = () => {
    let resultado = [...ejercicios];

    if (searchTerm.trim()) {
      const term = searchTerm.toLowerCase();
      resultado = resultado.filter(e => 
        e.nombre.toLowerCase().includes(term)
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

  // ============ VALIDATION ============
  const validateForm = (): boolean => {
    const errors: Record<string, string> = {};

    if (!formData.nombre.trim()) errors.nombre = 'El nombre es requerido';
    if (!(formData.descripcion ?? '').trim()) errors.descripcion = 'La descripción es requerida';
    if (formData.duracionMinutos === '' || formData.duracionSegundos === '') errors.duracion = 'La duración es requerida';
    if (formData.duracion <= 0) errors.duracion = 'La duración debe ser mayor a 0';
    if (formData.series === '' || formData.series < 1) errors.series = 'Las series deben ser al menos 1';
    if (formData.descansoEntreSeries === '' || formData.descansoEntreSeries < 0) errors.descansoEntreSeries = 'El descanso no puede ser negativo';

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const hasChanges = (): boolean => {
    if (!editingId || !originalFormData) return false;
    return JSON.stringify(formData) !== JSON.stringify(originalFormData);
  };

  // ============ CRUD OPERATIONS ============
  const handleSubmit = async () => {
    if (!validateForm()) {
      toast.error('Por favor completa todos los campos requeridos');
      return;
    }
    
    if (editingId) {
      setShowConfirmSave(true);
      return;
    }

    // Check for duplicates before creating
    const nombreNormalizado = formData.nombre.trim().toLowerCase();
    const ejercicioEnMismaEnfermedad = ejercicios.find(e =>
      e.nombre.toLowerCase() === nombreNormalizado && 
      e.enfermedad === formData.enfermedad
    );

    if (ejercicioEnMismaEnfermedad) {
      toast.error(`El ejercicio "${formData.nombre}" ya existe en ${formData.enfermedad}`);
      return;
    }

    // Check if exists in other disease
    const ejercicioEnOtraEnfermedad = ejercicios.find(e =>
      e.nombre.toLowerCase() === nombreNormalizado && 
      e.enfermedad !== formData.enfermedad
    );

    if (ejercicioEnOtraEnfermedad && !proceedWithDuplicate) {
      setDuplicateInfo({
        ejercicio: ejercicioEnOtraEnfermedad.nombre,
        enfermedad: ejercicioEnOtraEnfermedad.enfermedad
      });
      setShowDuplicateWarning(true);
      return;
    }

    await guardarEjercicio();
  };

  const guardarEjercicio = async () => {
    if (!validateForm()) return;

    try {
      setIsSubmitting(true);
      const minutos = typeof formData.duracionMinutos === 'number' ? formData.duracionMinutos : 0;
      const segundos = typeof formData.duracionSegundos === 'number' ? formData.duracionSegundos : 0;
      const duracionSegundos = (minutos * 60) + segundos;

      const dataToSend = {
        nombre: formData.nombre.trim(),
        descripcion: (formData.descripcion ?? '').trim() || undefined,
        duracion: duracionSegundos,
        intensidad: formData.intensidad,
        series: typeof formData.series === 'number' ? formData.series : 1,
        descansoEntreSeries: typeof formData.descansoEntreSeries === 'number' ? formData.descansoEntreSeries : 0,
        enfermedad: formData.enfermedad
      };

      if (editingId) {
        await ejercicioService.actualizarEjercicio(editingId, dataToSend);
        toast.success('Ejercicio actualizado');
      } else {
        await ejercicioService.crearEjercicio(dataToSend);
        toast.success('Ejercicio creado');
      }

      loadEjercicios(1, searchTerm, filterEnfermedad, filterIntensidad);
      setShowModal(false);
      setShowConfirmSave(false);
      setShowDuplicateWarning(false);
      setEditingId(null);
      setProceedWithDuplicate(false);
      resetForm();
    } catch (error: any) {
      toast.error(error.response?.data?.error || 'Error al guardar ejercicio');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleEdit = (ejercicio: EjercicioConAsignaciones) => {
    const minutos = Math.floor(ejercicio.duracion / 60);
    const segundos = ejercicio.duracion % 60;
    
    const editData: FormData = {
      nombre: ejercicio.nombre,
      descripcion: ejercicio.descripcion,
      duracion: ejercicio.duracion,
      duracionMinutos: minutos,
      duracionSegundos: segundos,
      intensidad: ejercicio.intensidad,
      series: ejercicio.series,
      descansoEntreSeries: ejercicio.descansoEntreSeries,
      enfermedad: ejercicio.enfermedad
    };

    setFormData(editData);
    setOriginalFormData(JSON.parse(JSON.stringify(editData)));
    setEditingId(ejercicio.id);
    setShowModal(true);
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('¿Estás seguro de que deseas eliminar este ejercicio?')) return;

    try {
      await ejercicioService.eliminarEjercicio(id);
      toast.success('Ejercicio eliminado');
      loadEjercicios(currentPage);
    } catch (error: any) {
      toast.error('Error al eliminar ejercicio');
    }
  };

  const handleCloseModal = () => {
    if (hasChanges()) {
      setShowConfirmCancel(true);
    } else {
      setShowModal(false);
      setEditingId(null);
      resetForm();
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

  const handleSearch = () => {
    setSearchTerm(searchInput);
    loadEjercicios(1, searchInput, filterEnfermedad, filterIntensidad);
  };

  const handleApplyFilters = () => {
    setFilterEnfermedad(filterEnfermedad_temp);
    setFilterIntensidad(filterIntensidad_temp);
    loadEjercicios(1, searchTerm, filterEnfermedad_temp, filterIntensidad_temp);
  };

  const handleClearFilters = () => {
    // Resetear estados temporales
    setSearchInput('');
    setFilterEnfermedad_temp('');
    setFilterIntensidad_temp('');
    // Resetear estados aplicados
    setSearchTerm('');
    setFilterEnfermedad('');
    setFilterIntensidad('');
    // Recargar ejercicios sin filtros
    loadEjercicios(1, '', '', '');
  };

  // ============ PACIENTE ASSIGNMENT ============
  const handleAsignarPaciente = async (ejercicioId: number, pacienteId: number) => {
    try {
      await asignacionService.crearAsignacion({ ejercicioId, usuarioId: pacienteId });
      toast.success('Paciente asignado');
      loadEjercicios(currentPage);
    } catch (error: any) {
      toast.error('Error al asignar paciente');
    }
  };

  const handleDesasignarPaciente = async (ejercicioId: number, pacienteId: number) => {
    try {
      await asignacionService.eliminarAsignacionPaciente(ejercicioId, pacienteId);
      toast.success('Paciente desasignado');
      loadEjercicios(currentPage);
    } catch (error: any) {
      toast.error('Error al desasignar paciente');
    }
  };

  // ============ RENDERING ============
  if (loading) {
    return (
      <Layout>
        <Loading message="Cargando ejercicios..." fullHeight />
      </Layout>
    );
  }

  const getIntensidadColor = (intensidad: Intensidad): 'danger' | 'warning' | 'success' => {
    switch(intensidad) {
      case 'Alta': return 'danger';
      case 'Media': return 'warning';
      case 'Baja': return 'success';
      default: return 'warning';
    }
  };

  const dataGridItems = filtrados.map(ejercicio => {
    const enfermedadColor = getEnfermedadColor(ejercicio.enfermedad);
    return {
      id: ejercicio.id,
      title: ejercicio.nombre,
      description: ejercicio.descripcion,
      badges: [
        { 
          label: ejercicio.enfermedad, 
          backgroundColor: enfermedadColor,
          textColor: '#ffffff'
        },
        { label: `${ejercicio.intensidad}`, color: getIntensidadColor(ejercicio.intensidad) },
        { label: `${ejercicio.totalAsignados || 0} pacientes`, color: 'default' as const }
      ],
      onEdit: () => handleEdit(ejercicio),
      onDelete: () => handleDelete(ejercicio.id),
      onClick: () => setSelectedEjercicioDetalle(ejercicio)
    };
  });

  return (
    <Layout>
      <PageTransition>
        <div className="gestion-ejercicios-page">
          {/* Header */}
          <div className="gestion-header">
            <div className="header-top">
              <button onClick={() => navigate('/dashboard')} className="back-button">
                <ArrowLeft size={20} />
                Volver
              </button>
              <h1 className="gestion-title">Gestión de Ejercicios</h1>
              <button
                onClick={() => { resetForm(); setEditingId(null); setShowModal(true); }}
                className="btn-primary"
              >
                <Plus size={18} />
                Nuevo Ejercicio
              </button>
            </div>

            {/* Search & Filter */}
            <SearchFilter
              searchValue={searchInput}
              onSearchChange={setSearchInput}
              onSearchSubmit={handleSearch}
              filterOptions={[
                {
                  name: 'enfermedad',
                  label: 'Enfermedad',
                  type: 'select',
                  options: ENFERMEDADES.map(e => ({ label: e, value: e })),
                  value: filterEnfermedad_temp
                },
                {
                  name: 'intensidad',
                  label: 'Intensidad',
                  type: 'select',
                  options: INTENSIDADES.map(i => ({ label: i, value: i })),
                  value: filterIntensidad_temp
                }
              ]}
              onFilterChange={(name, value) => {
                if (name === 'enfermedad') setFilterEnfermedad_temp(value as Enfermedad | '');
                if (name === 'intensidad') setFilterIntensidad_temp(value as Intensidad | '');
              }}
              onApplyFilters={handleApplyFilters}
              onClearFilters={handleClearFilters}
              isDarkMode={isDarkMode}
              placeholder="Buscar ejercicios..."
            />
          </div>

          {/* Data Grid */}
          <DataGrid
            items={dataGridItems}
            isDarkMode={isDarkMode}
            columns={3}
            emptyMessage="No hay ejercicios disponibles"
          />

          {/* Pagination */}
          {totalPages > 1 && (
            <Pagination
              currentPage={currentPage}
              totalPages={totalPages}
              onPageChange={(page) => loadEjercicios(page)}
              itemsPerPage={ITEMS_PER_PAGE}
              totalItems={filtrados.length}
              isDarkMode={isDarkMode}
            />
          )}

          {/* CRUD Modal */}
          <CRUDModal
            isOpen={showModal}
            title={editingId ? 'Editar Ejercicio' : 'Nuevo Ejercicio'}
            onSubmit={handleSubmit}
            onCancel={handleCloseModal}
            onClose={handleCloseModal}
            errors={formErrors}
            isLoading={isSubmitting}
            submitLabel={editingId ? 'Actualizar' : 'Crear'}
            isDarkMode={isDarkMode}
          >
            <div className="form-columns">
              {/* COLUMNA IZQUIERDA */}
              <div className="form-columns-left">
                {/* Nombre */}
                <div className="form-group">
                  <label htmlFor="nombre">Nombre *</label>
                  <input
                    id="nombre"
                    type="text"
                    value={formData.nombre}
                    onChange={(e) => setFormData({ ...formData, nombre: e.target.value })}
                    className="form-input"
                    placeholder="Nombre del ejercicio"
                  />
                  {formErrors.nombre && <span className="error-message">{formErrors.nombre}</span>}
                </div>

                {/* Descripción */}
                <div className="form-group">
                  <label htmlFor="descripcion">Descripción *</label>
                  <textarea
                    id="descripcion"
                    value={formData.descripcion}
                    onChange={(e) => setFormData({ ...formData, descripcion: e.target.value })}
                    className="form-textarea"
                    placeholder="Describe el ejercicio..."
                    rows={3}
                  />
                  {formErrors.descripcion && <span className="error-message">{formErrors.descripcion}</span>}
                </div>

                {/* Enfermedad */}
                <div className="form-group">
                  <label htmlFor="enfermedad">Enfermedad *</label>
                  <select
                    id="enfermedad"
                    value={formData.enfermedad}
                    onChange={(e) => setFormData({ ...formData, enfermedad: e.target.value as Enfermedad })}
                    className="form-input"
                  >
                    {ENFERMEDADES.map(e => <option key={e} value={e}>{e}</option>)}
                  </select>
                </div>

                {/* Intensidad */}
                <div className="form-group">
                  <label htmlFor="intensidad">Intensidad *</label>
                  <select
                    id="intensidad"
                    value={formData.intensidad}
                    onChange={(e) => setFormData({ ...formData, intensidad: e.target.value as Intensidad })}
                    className="form-input"
                  >
                    {INTENSIDADES.map(i => <option key={i} value={i}>{i}</option>)}
                  </select>
                </div>
              </div>

              {/* COLUMNA DERECHA */}
              <div className="form-columns-right">
                {/* Imagen */}
                <div className="form-group">
                  <label htmlFor="imagen">Imagen</label>
                  <input
                    id="imagen"
                    type="file"
                    accept="image/*"
                    onChange={(e) => setImageFile(e.target.files?.[0] || null)}
                    className="form-input"
                  />
                </div>

                {/* Duración */}
                <div className="form-group">
                  <label htmlFor="duracion">Duración (MM:SS) *</label>
                  <div className="duration-input">
                    <input
                      type="number"
                      value={formData.duracionMinutos}
                      onChange={(e) => {
                        const mins = e.target.value === '' ? '' : parseInt(e.target.value);
                        const minVal = typeof mins === 'number' ? mins * 60 : 0;
                        const secVal = typeof formData.duracionSegundos === 'number' ? formData.duracionSegundos : 0;
                        setFormData({ ...formData, duracionMinutos: mins, duracion: minVal + secVal });
                      }}
                      min="0"
                      max="999"
                      className="form-input"
                      placeholder="MM"
                    />
                    <span>:</span>
                    <input
                      type="number"
                      value={formData.duracionSegundos}
                      onChange={(e) => {
                        const secs = e.target.value === '' ? '' : Math.min(parseInt(e.target.value), 59);
                        const secVal = typeof secs === 'number' ? secs : 0;
                        const minVal = typeof formData.duracionMinutos === 'number' ? formData.duracionMinutos * 60 : 0;
                        setFormData({ ...formData, duracionSegundos: secs, duracion: minVal + secVal });
                      }}
                      min="0"
                      max="59"
                      className="form-input"
                      placeholder="SS"
                    />
                  </div>
                </div>

                {/* Series */}
                <div className="form-group">
                  <label htmlFor="series">Series *</label>
                  <input
                    id="series"
                    type="number"
                    value={formData.series}
                    onChange={(e) => setFormData({ ...formData, series: e.target.value === '' ? '' : parseInt(e.target.value) })}
                    min="1"
                    className="form-input"
                  />
                </div>

                {/* Descanso */}
                <div className="form-group">
                  <label htmlFor="descanso">Descanso (seg) *</label>
                  <input
                    id="descanso"
                    type="number"
                    value={formData.descansoEntreSeries}
                    onChange={(e) => setFormData({ ...formData, descansoEntreSeries: e.target.value === '' ? '' : Math.max(0, parseInt(e.target.value)) })}
                    min="0"
                    className="form-input"
                  />
                </div>
              </div>
            </div>
          </CRUDModal>

          {/* Confirm Save Modal */}
          <ConfirmModal
            isOpen={showConfirmSave}
            title="¿Confirmar cambios?"
            message="¿Estás seguro de que deseas actualizar este ejercicio?"
            onConfirm={guardarEjercicio}
            onCancel={() => setShowConfirmSave(false)}
            isDarkMode={isDarkMode}
            isLoading={isSubmitting}
          />

          {/*  Duplicate Warning Modal */}
          {showDuplicateWarning && duplicateInfo && (
            <ConfirmModal
              isOpen={showDuplicateWarning}
              title="⚠️ Ejercicio duplicado"
              message={`El ejercicio "${duplicateInfo.ejercicio}" ya existe en "${duplicateInfo.enfermedad}". ¿Deseas crear un nuevo ejercicio en "${formData.enfermedad}" de todas formas?`}
              onConfirm={async () => {
                setShowDuplicateWarning(false);
                setProceedWithDuplicate(true);
                await guardarEjercicio();
              }}
              onCancel={() => {
                setShowDuplicateWarning(false);
                setDuplicateInfo(null);
              }}
              confirmLabel="Sí, crear ejercicio"
              cancelLabel="No, cancelar"
              isDarkMode={isDarkMode}
            />
          )}

          {/* Confirm Cancel Modal */}
          <ConfirmModal
            isOpen={showConfirmCancel}
            title="¿Descartar cambios?"
            message="Tienes cambios sin guardar. ¿Deseas descartarlos?"
            onConfirm={() => {
              setShowConfirmCancel(false);
              setShowModal(false);
              setEditingId(null);
              resetForm();
            }}
            onCancel={() => setShowConfirmCancel(false)}
            confirmLabel="Descartar"
            cancelLabel="Continuar editando"
            isDarkMode={isDarkMode}
          />

          {/* Detalle Modal */}
          {selectedEjercicioDetalle && (
            <ExerciseDetailsModal
              ejercicio={selectedEjercicioDetalle}
              pacientes={pacientes}
              isDarkMode={isDarkMode}
              onClose={() => setSelectedEjercicioDetalle(null)}
              onRemoveAssignment={(pacienteId) => {
                handleDesasignarPaciente(selectedEjercicioDetalle.id, pacienteId);
              }}
            />
          )}
        </div>
      </PageTransition>
    </Layout>
  );
};

export default GestionEjercicios;
