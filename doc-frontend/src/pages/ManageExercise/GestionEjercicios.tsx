import React, { useEffect, useState } from 'react';
import { Plus, ArrowLeft } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import api from '../../services/api';
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

interface FormData extends Omit<CreateEjercicioRequest, 'series' | 'descansoEntreSeries'> {
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
  const [showConfirmDelete, setShowConfirmDelete] = useState(false);
  const [deleteEjercicioId, setDeleteEjercicioId] = useState<number | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [selectedEjercicioDetalle, setSelectedEjercicioDetalle] = useState<EjercicioConAsignaciones | null>(null);

  // Search & Filter states - Live input (being edited)
  const [searchInput, setSearchInput] = useState('');
  const [filterEnfermedad_temp, setFilterEnfermedad_temp] = useState<Enfermedad | ''>('');
  const [filterIntensidad_temp, setFilterIntensidad_temp] = useState<Intensidad | ''>('');
  const [sortBy_temp, setSortBy_temp] = useState<'az' | 'za' | 'reciente' | 'antiguo' | ''>('');
  
  // Search & Filter states - Applied (used in loadEjercicios)
  const [searchTerm, setSearchTerm] = useState('');
  const [filterEnfermedad, setFilterEnfermedad] = useState<Enfermedad | ''>('');
  const [filterIntensidad, setFilterIntensidad] = useState<Intensidad | ''>('');
  const [sortBy, setSortBy] = useState<'az' | 'za' | 'reciente' | 'antiguo' | ''>('');

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
    enfermedad: 'Hipertensión',
    tipo: 'POR_ENFERMEDAD'
  });

  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [originalFormData, setOriginalFormData] = useState<FormData | null>(null);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string | null>(null);

  // Duplicate validation states
  const [showDuplicateWarning, setShowDuplicateWarning] = useState(false);
  const [duplicateInfo, setDuplicateInfo] = useState<{ ejercicio: string; enfermedad: Enfermedad } | null>(null);
  const [proceedWithDuplicate, setProceedWithDuplicate] = useState(false);

  // ✅ Validación de pacientes para ejercicios "Por Pacientes"
  const [showNoPacientesWarning, setShowNoPacientesWarning] = useState(false);
  const [pacientesSeleccionadosTemp, setPacientesSeleccionadosTemp] = useState<number[]>([]);

  // Pagination & Data
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [pacientes, setPacientes] = useState<any[]>([]);

  // Vista por Paciente
  const [vistaActual, setVistaActual] = useState<'todos' | 'por-paciente'>('todos');
  const [pacienteSeleccionado, setPacienteSeleccionado] = useState<number | null>(null);
  
  // ✅ Todos los ejercicios (sin paginación) - para calcular asignaciones globales
  const [ejerciciosTotal, setEjerciciosTotal] = useState<EjercicioConAsignaciones[]>([]);

  // ✅ Estados para búsqueda y expansión de pacientes
  const [pacientesBusqueda, setPacientesBusqueda] = useState('');
  const [mostrarTodosPacientes, setMostrarTodosPacientes] = useState(false);

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
    loadEjerciciosTotal(); // ✅ Cargar todos los ejercicios para asignaciones globales
  }, []);

  useEffect(() => {
    aplicarFiltros();
  }, [ejercicios, searchTerm, filterEnfermedad, filterIntensidad, sortBy]);

  // ✅ Real-time validation: validate as user types
  useEffect(() => {
    if (showModal) {
      validateForm();
    }
  }, [formData, showModal]);

  // ============ DATA LOADING ============
  const loadEjercicios = async (
    page: number = 1, 
    search: string = searchTerm, 
    enf: Enfermedad | '' = filterEnfermedad, 
    intensidad: Intensidad | '' = filterIntensidad
  ): Promise<EjercicioConAsignaciones[]> => {
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
      
      // ✅ Retornar los ejercicios cargados para usar en callbacks
      return datos.ejercicios;
    } catch (error: any) {
      console.error('Error cargando ejercicios:', error);
      toast.error('Error al cargar ejercicios');
      return [];
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

  // ✅ Cargar TODOS los ejercicios sin paginación (para calcular asignaciones globales)
  const loadEjerciciosTotal = async () => {
    try {
      const datos = await ejercicioService.listarEjercicios({
        page: 1,
        limit: 9999 // Cargar todos sin restricción
      });
      setEjerciciosTotal(datos.ejercicios);
    } catch (error: any) {
      console.error('Error cargando ejercicios total:', error);
    }
  };

  // ✅ Calcular cuántos ejercicios tiene asignado cada paciente
  const pacientesConConteo = React.useMemo(() => {
    const mapa = new Map<number, { paciente: any; ejercicios: EjercicioConAsignaciones[] }>();
    
    // Inicializar todos los pacientes
    pacientes.forEach(p => {
      mapa.set(p.id, { paciente: p, ejercicios: [] });
    });
    
    // ✅ Contar ejercicios por paciente usando TODOS los ejercicios (no solo página actual)
    ejerciciosTotal.forEach(ejercicio => {
      ejercicio.pacientesAsignados?.forEach(paciente => {
        const entry = mapa.get(paciente.id);
        if (entry) {
          entry.ejercicios.push(ejercicio);
        }
      });
    });
    
    // Crear array y ordenar por cantidad de ejercicios (desc)
    return Array.from(mapa.values())
      .filter(p => p.ejercicios.length > 0) // Solo mostrar pacientes con ejercicios
      .sort((a, b) => b.ejercicios.length - a.ejercicios.length);
  }, [ejerciciosTotal, pacientes]);

  // ✅ Obtener ejercicios del paciente seleccionado
  const ejerciciosPacienteSeleccionado = React.useMemo(() => {
    if (vistaActual !== 'por-paciente' || !pacienteSeleccionado) return filtrados;
    
    // ✅ Usar ejerciciosTotal para mostrar TODOS los ejercicios del paciente, no solo los de la página
    return ejerciciosTotal.filter(e => 
      e.pacientesAsignados?.some(p => p.id === pacienteSeleccionado)
    );
  }, [ejerciciosTotal, pacienteSeleccionado, vistaActual]);

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

    // ✅ Aplicar ordenamiento
    if (sortBy === 'az') {
      resultado.sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));
    } else if (sortBy === 'za') {
      resultado.sort((a, b) => b.nombre.localeCompare(a.nombre, 'es'));
    } else if (sortBy === 'reciente') {
      resultado.sort((a, b) => {
        const dateA = a.creadoEn ? new Date(a.creadoEn).getTime() : 0;
        const dateB = b.creadoEn ? new Date(b.creadoEn).getTime() : 0;
        return dateB - dateA;
      });
    } else if (sortBy === 'antiguo') {
      resultado.sort((a, b) => {
        const dateA = a.creadoEn ? new Date(a.creadoEn).getTime() : 0;
        const dateB = b.creadoEn ? new Date(b.creadoEn).getTime() : 0;
        return dateA - dateB;
      });
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
        enfermedad: (ejercicioEnOtraEnfermedad.enfermedad || 'General') as Enfermedad
      });
      setShowDuplicateWarning(true);
      return;
    }

    await guardarEjercicio();
  };

  const guardarEjercicio = async () => {
    if (!validateForm()) return;

    // ✅ Validar que si es "Por Pacientes", haya pacientes asignados
    if (formData.tipo === 'POR_PACIENTES' && pacientesSeleccionadosTemp.length === 0 && !editingId) {
      setShowNoPacientesWarning(true);
      setIsSubmitting(false);
      return;
    }

    try {
      setIsSubmitting(true);
      const minutos = typeof formData.duracionMinutos === 'number' ? formData.duracionMinutos : 0;
      const segundos = typeof formData.duracionSegundos === 'number' ? formData.duracionSegundos : 0;
      const duracionSegundos = (minutos * 60) + segundos;

      let urlImagen: string | undefined = undefined;

      // ✅ SUBIR IMAGEN AL BACKEND (si hay nueva imagen)
      if (imageFile) {
        try {
          toast.loading('Subiendo imagen...');
          
          // Crear FormData
          const formDataMultipart = new FormData();
          formDataMultipart.append('imagen', imageFile);
          
          // Enviar al backend usando axios (con token automático)
          const response = await api.post('/api/upload/ejercicio-imagen', formDataMultipart, {
            headers: {
              'Content-Type': 'multipart/form-data'
            }
          });

          urlImagen = response.data.imageUrl;

          // Si hay imagen antigua, borrarla
          if (editingId && formData.urlImagen) {
            try {
              const fileName = formData.urlImagen.split('/').pop() || '';
              await api.delete(`/api/upload/ejercicio-imagen/${encodeURIComponent(fileName)}`).catch(() => {}); 
            } catch (deleteError) {
              console.warn('⚠️ No se pudo borrar imagen antigua:', deleteError);
            }
          }
        } catch (imageError: any) {
          toast.dismiss();
          toast.error('Error al subir imagen: ' + imageError.message);
          setIsSubmitting(false);
          return;
        }
      } else if (editingId) {
        // Si estamos editando y NO hay imagen nueva, mantener la antigua
        urlImagen = formData.urlImagen || undefined;
      }

      toast.dismiss();

      const dataToSend: any = {
        nombre: formData.nombre.trim(),
        descripcion: (formData.descripcion ?? '').trim() || undefined,
        duracion: duracionSegundos,
        intensidad: formData.intensidad,
        series: typeof formData.series === 'number' ? formData.series : 1,
        descansoEntreSeries: typeof formData.descansoEntreSeries === 'number' ? formData.descansoEntreSeries : 0,
        tipo: formData.tipo || 'POR_ENFERMEDAD'
      };

      // ✅ Solo incluir enfermedad si es "POR_ENFERMEDAD"
      if (formData.tipo === 'POR_ENFERMEDAD') {
        dataToSend.enfermedad = formData.enfermedad;
      }

      // Incluir imagen si existe
      if (urlImagen) {
        dataToSend.urlImagen = urlImagen;
      }

      if (editingId) {
        await ejercicioService.actualizarEjercicio(editingId, dataToSend);
        toast.success('Ejercicio actualizado');
      } else {
        const nuevoEjercicio = await ejercicioService.crearEjercicio(dataToSend);
        toast.success('Ejercicio creado');
        
        // ✅ Si es "Por Pacientes", asignar pacientes después de crear
        if (formData.tipo === 'POR_PACIENTES' && pacientesSeleccionadosTemp.length > 0) {
          
          // Asignar pacientes CON MANEJO DE ERRORES
          let asignacionesExitosas = 0;
          let asignacionesFallidas: {pacienteId: number, error: string}[] = [];
          
          for (const pacienteId of pacientesSeleccionadosTemp) {
            try {
              await asignacionService.crearAsignacion({
                ejercicioId: nuevoEjercicio.id,
                usuarioId: pacienteId
              });
              asignacionesExitosas++;
            } catch (assignError: any) {
              asignacionesFallidas.push({
                pacienteId,
                error: assignError.response?.data?.error || assignError.message
              });
            }
          }
          
          loadEjerciciosTotal();
          
          // Toast con información detallada
          if (asignacionesExitosas > 0 && asignacionesFallidas.length === 0) {
            toast.success(`✅ Ejercicio creado y asignado a ${asignacionesExitosas} paciente(s)`);
          } else if (asignacionesExitosas > 0 && asignacionesFallidas.length > 0) {
            toast.error(`⚠️ Asignado a ${asignacionesExitosas} de ${pacientesSeleccionadosTemp.length} pacientes. ${asignacionesFallidas.length} tuvieron error.`);
          } else if (asignacionesFallidas.length > 0) {
            toast.error(`❌ Error en todas las asignaciones: ${asignacionesFallidas[0].error}`);
            return; // No cerrar modal si todas fallaron
          }
        }
      }

      loadEjercicios(1, searchTerm, filterEnfermedad, filterIntensidad);
      loadEjerciciosTotal(); // ✅ Actualizar lista global
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
      enfermedad: ejercicio.enfermedad,
      urlImagen: ejercicio.urlImagen,
      urlVideo: ejercicio.urlVideo
    };

    setFormData(editData);
    setOriginalFormData(JSON.parse(JSON.stringify(editData)));
    setEditingId(ejercicio.id);
    setImageFile(null); // Reset imagen para poder seleccionar una nueva
    setImagePreview(null); // Reset preview
    setShowModal(true);
  };

  const handleDelete = (id: number) => {
    setDeleteEjercicioId(id);
    setShowConfirmDelete(true);
  };

  const confirmDelete = async () => {
    if (!deleteEjercicioId) return;

    try {
      await ejercicioService.eliminarEjercicio(deleteEjercicioId);
      toast.success('Ejercicio eliminado');
      setShowConfirmDelete(false);
      setDeleteEjercicioId(null);
      loadEjercicios(currentPage);
      loadEjerciciosTotal(); // ✅ Actualizar lista global
    } catch (error: any) {
      toast.error('Error al eliminar ejercicio');
      setShowConfirmDelete(false);
      setDeleteEjercicioId(null);
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
      enfermedad: 'Hipertensión',
      tipo: 'POR_ENFERMEDAD'
    });
    setImageFile(null);
    setImagePreview(null);
    setEditingId(null);
    setFormErrors({});
    setOriginalFormData(null);
    setPacientesSeleccionadosTemp([]);
    setPacientesBusqueda('');
    setMostrarTodosPacientes(false);
  };

  const handleSearch = () => {
    setSearchTerm(searchInput);
    loadEjercicios(1, searchInput, filterEnfermedad, filterIntensidad);
  };

  const handleApplyFilters = () => {
    setFilterEnfermedad(filterEnfermedad_temp);
    setFilterIntensidad(filterIntensidad_temp);
    setSortBy(sortBy_temp);
    loadEjercicios(1, searchTerm, filterEnfermedad_temp, filterIntensidad_temp);
  };

  const handleClearFilters = () => {
    // Resetear estados temporales
    setSearchInput('');
    setFilterEnfermedad_temp('');
    setFilterIntensidad_temp('');
    setSortBy_temp('');
    // Resetear estados aplicados
    setSearchTerm('');
    setFilterEnfermedad('');
    setFilterIntensidad('');
    setSortBy('');
    // Recargar ejercicios sin filtros
    loadEjercicios(1, '', '', '');
  };

  // ============ PACIENTE ASSIGNMENT ============
  // Asignar pacientes a un ejercicio
  const handleAsignarPacientes = async (ejercicioId: number, pacienteIds: number[]) => {
    try {
      toast.loading('Asignando pacientes...');
      
      let asignados = 0;
      let duplicados = 0;

      // Crear asignación para cada paciente
      for (const pacienteId of pacienteIds) {
        try {
          await asignacionService.crearAsignacion({
            ejercicioId,
            usuarioId: pacienteId
          });
          asignados++;
        } catch (error: any) {
          // Si es duplicado, contar pero no fallar
          if (error.response?.data?.skipToast || error.response?.status === 400) {
            duplicados++;
          } else {
            throw error;
          }
        }
      }
      
      toast.dismiss();
      
      if (asignados > 0) {
        toast.success(`Ejercicio asignado a ${asignados} paciente(s)`);
      }
      if (duplicados > 0) {
        toast.success(`${duplicados} paciente(s) ya tenía(n) este ejercicio asignado`);
      }
      
      // ✅ Recargar datos y usar el resultado directamente
      const ejerciciosActualizados = await loadEjercicios(currentPage);
      loadEjerciciosTotal(); // ✅ Actualizar lista global
      
      // Buscar el ejercicio actualizado en los nuevos datos
      const ejercicioActualizado = ejerciciosActualizados.find(e => e.id === ejercicioId);
      if (ejercicioActualizado) {
        setSelectedEjercicioDetalle(ejercicioActualizado);
      }
    } catch (error: any) {
      toast.dismiss();
      toast.error('Error al asignar pacientes: ' + error.message);
    }
  };

  // Nota: La desasignación de pacientes también se maneja aquí
  const handleDesasignarPaciente = async (ejercicioId: number, pacienteId: number) => {
    try {
      await asignacionService.eliminarAsignacionPaciente(ejercicioId, pacienteId);
      toast.success('Paciente desasignado');
      
      // Recargar ejercicios
      const ejerciciosActualizados = await loadEjercicios(currentPage);
      loadEjerciciosTotal(); // ✅ Actualizar lista global
      
      // ✅ Actualizar el modal con los datos frescos (sin cerrar)
      if (selectedEjercicioDetalle) {
        const ejercicioActualizado = ejerciciosActualizados.find(e => e.id === ejercicioId);
        if (ejercicioActualizado) {
          setSelectedEjercicioDetalle(ejercicioActualizado);
        }
      }
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

  const dataGridItems = (vistaActual === 'por-paciente' ? ejerciciosPacienteSeleccionado : filtrados).map(ejercicio => {
    const enfermedadColor = getEnfermedadColor(ejercicio.enfermedad);
    return {
      id: ejercicio.id,
      title: ejercicio.nombre,
      description: ejercicio.descripcion,
      badges: [
        ...( ejercicio.enfermedad ? [{
          label: ejercicio.enfermedad,
          backgroundColor: enfermedadColor,
          textColor: '#ffffff'
        }] : []),
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

            {/* Vista Tabs */}
            <div style={{ 
              display: 'flex', 
              gap: '0.5rem', 
              marginBottom: '1rem',
              borderBottom: '2px solid #e2e8f0',
              paddingBottom: '0.5rem'
            }}>
              <button
                onClick={() => {
                  setVistaActual('todos');
                  setPacienteSeleccionado(null);
                }}
                style={{
                  padding: '0.5rem 1rem',
                  borderTop: 'none',
                  borderRight: 'none',
                  borderLeft: 'none',
                  borderBottom: vistaActual === 'todos' ? '3px solid #3b82f6' : 'none',
                  backgroundColor: vistaActual === 'todos' ? 'rgba(59, 130, 246, 0.1)' : 'transparent',
                  color: vistaActual === 'todos' ? '#3b82f6' : '#64748b',
                  cursor: 'pointer',
                  fontWeight: 600,
                  transition: 'all 0.2s',
                  marginBottom: '-2px'
                }}
              >
                Todos los Ejercicios
              </button>
              <button
                onClick={() => setVistaActual('por-paciente')}
                style={{
                  padding: '0.5rem 1rem',
                  borderTop: 'none',
                  borderRight: 'none',
                  borderLeft: 'none',
                  borderBottom: vistaActual === 'por-paciente' ? '3px solid #3b82f6' : 'none',
                  backgroundColor: vistaActual === 'por-paciente' ? 'rgba(59, 130, 246, 0.1)' : 'transparent',
                  color: vistaActual === 'por-paciente' ? '#3b82f6' : '#64748b',
                  cursor: 'pointer',
                  fontWeight: 600,
                  transition: 'all 0.2s',
                  marginBottom: '-2px'
                }}
              >
                Por Paciente ({pacientesConConteo.length})
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
                },
                {
                  name: 'sortBy',
                  label: 'Ordenar',
                  type: 'select',
                  options: [
                    { label: 'A - Z', value: 'az' },
                    { label: 'Z - A', value: 'za' },
                    { label: 'Más reciente', value: 'reciente' },
                    { label: 'Más antiguo', value: 'antiguo' }
                  ],
                  value: sortBy_temp
                }
              ]}
              onFilterChange={(name, value) => {
                if (name === 'enfermedad') setFilterEnfermedad_temp(value as Enfermedad | '');
                if (name === 'intensidad') setFilterIntensidad_temp(value as Intensidad | '');
                if (name === 'sortBy') setSortBy_temp(value as 'az' | 'za' | 'reciente' | 'antiguo' | '');
              }}
              onApplyFilters={handleApplyFilters}
              onClearFilters={handleClearFilters}
              isDarkMode={isDarkMode}
              placeholder="Buscar ejercicios..."
            />
          </div>

          {/* Vista Por Paciente con Sidebar */}
          {vistaActual === 'por-paciente' ? (
            <div style={{ display: 'grid', gridTemplateColumns: '280px 1fr', gap: '1.5rem' }}>
              {/* SIDEBAR - Pacientes */}
              <div style={{
                backgroundColor: isDarkMode ? '#1e293b' : '#f8fafc',
                borderRadius: '0.75rem',
                padding: '1.5rem',
                maxHeight: '600px',
                overflowY: 'auto',
                border: `1px solid ${isDarkMode ? '#334155' : '#e2e8f0'}`
              }}>
                <h3 style={{ 
                  marginTop: 0, 
                  marginBottom: '1rem',
                  color: isDarkMode ? '#e2e8f0' : '#1e293b',
                  fontSize: '1rem',
                  fontWeight: 600
                }}>
                  Pacientes
                </h3>
                {pacientesConConteo.length === 0 ? (
                  <div style={{ 
                    textAlign: 'center', 
                    color: isDarkMode ? '#94a3b8' : '#64748b',
                    fontSize: '0.875rem',
                    padding: '2rem 0'
                  }}>
                    No hay pacientes con asignaciones
                  </div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    {pacientesConConteo.map(({ paciente, ejercicios }) => (
                      <button
                        key={paciente.id}
                        onClick={() => setPacienteSeleccionado(
                          pacienteSeleccionado === paciente.id ? null : paciente.id
                        )}
                        style={{
                          padding: '0.75rem 1rem',
                          backgroundColor: pacienteSeleccionado === paciente.id 
                            ? '#3b82f6' 
                            : isDarkMode ? '#334155' : '#e2e8f0',
                          color: pacienteSeleccionado === paciente.id 
                            ? '#ffffff' 
                            : isDarkMode ? '#e2e8f0' : '#1e293b',
                          border: 'none',
                          borderRadius: '0.5rem',
                          textAlign: 'left',
                          cursor: 'pointer',
                          fontWeight: 500,
                          transition: 'all 0.2s',
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center'
                        }}
                        onMouseEnter={(e) => {
                          if (pacienteSeleccionado !== paciente.id) {
                            e.currentTarget.style.backgroundColor = isDarkMode ? '#475569' : '#cbd5e1';
                          }
                        }}
                        onMouseLeave={(e) => {
                          if (pacienteSeleccionado !== paciente.id) {
                            e.currentTarget.style.backgroundColor = isDarkMode ? '#334155' : '#e2e8f0';
                          }
                        }}
                      >
                        <span>{paciente.nombre} {paciente.apellido}</span>
                        <span style={{
                          backgroundColor: pacienteSeleccionado === paciente.id 
                            ? 'rgba(255,255,255,0.3)' 
                            : isDarkMode ? 'rgba(59,130,246,0.3)' : 'rgba(59,130,246,0.2)',
                          padding: '0.25rem 0.5rem',
                          borderRadius: '0.25rem',
                          fontSize: '0.75rem',
                          fontWeight: 600,
                          minWidth: '1.5rem',
                          textAlign: 'center'
                        }}>
                          {ejercicios.length}
                        </span>
                      </button>
                    ))}
                  </div>
                )}
              </div>

              {/* MAIN - Ejercicios del Paciente */}
              <div>
                {pacienteSeleccionado && (
                  <div style={{ marginBottom: '1rem', color: isDarkMode ? '#cbd5e1' : '#475569' }}>
                    <p style={{ margin: 0, fontSize: '0.875rem' }}>
                      Mostrando {ejerciciosPacienteSeleccionado.length} ejercicio(s) asignados
                    </p>
                  </div>
                )}
                <DataGrid
                  items={dataGridItems}
                  isDarkMode={isDarkMode}
                  columns={2}
                  emptyMessage={pacienteSeleccionado ? 'Este paciente no tiene ejercicios asignados' : 'Selecciona un paciente'}
                />
              </div>
            </div>
          ) : (
            // Vista Todos Los Ejercicios
            <>
              {/* Data Grid */}
              <DataGrid
                items={dataGridItems}
                isDarkMode={isDarkMode}
                columns={3}
                emptyMessage="No hay ejercicios disponibles"
              />
            </>
          )}

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

                {/* ✅ Tipo de Ejercicio - MEJORADO CON DARK MODE */}
                <div className="form-group">
                  <label style={{ color: isDarkMode ? '#e2e8f0' : undefined }}>Tipo de Ejercicio *</label>
                  <div style={{ display: 'flex', gap: '1rem', marginTop: '0.75rem' }}>
                    <button
                      type="button"
                      onClick={() => setFormData({ ...formData, tipo: 'POR_ENFERMEDAD' })}
                      disabled={editingId ? true : false}
                      style={{
                        flex: 1,
                        padding: '0.75rem 1rem',
                        borderRadius: '8px',
                        border: formData.tipo === 'POR_ENFERMEDAD' ? '2px solid #3b82f6' : `2px solid ${isDarkMode ? '#475569' : '#e5e7eb'}`,
                        backgroundColor: formData.tipo === 'POR_ENFERMEDAD' ? 'rgba(59, 130, 246, 0.1)' : isDarkMode ? '#1e293b' : '#f9fafb',
                        color: formData.tipo === 'POR_ENFERMEDAD' ? '#3b82f6' : isDarkMode ? '#cbd5e1' : '#64748b',
                        fontWeight: formData.tipo === 'POR_ENFERMEDAD' ? '600' : '500',
                        cursor: editingId ? 'default' : 'pointer',
                        transition: 'all 0.2s',
                        opacity: editingId ? 0.6 : 1
                      }}
                    >
                      Por Enfermedad
                    </button>
                    <button
                      type="button"
                      onClick={() => setFormData({ ...formData, tipo: 'POR_PACIENTES' })}
                      disabled={editingId ? true : false}
                      style={{
                        flex: 1,
                        padding: '0.75rem 1rem',
                        borderRadius: '8px',
                        border: formData.tipo === 'POR_PACIENTES' ? '2px solid #8b5cf6' : `2px solid ${isDarkMode ? '#475569' : '#e5e7eb'}`,
                        backgroundColor: formData.tipo === 'POR_PACIENTES' ? 'rgba(139, 92, 246, 0.1)' : isDarkMode ? '#1e293b' : '#f9fafb',
                        color: formData.tipo === 'POR_PACIENTES' ? '#8b5cf6' : isDarkMode ? '#cbd5e1' : '#64748b',
                        fontWeight: formData.tipo === 'POR_PACIENTES' ? '600' : '500',
                        cursor: editingId ? 'default' : 'pointer',
                        transition: 'all 0.2s',
                        opacity: editingId ? 0.6 : 1
                      }}
                    >
                      Por Pacientes
                    </button>
                  </div>
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

                {/* ✅ Enfermedad (solo si es POR_ENFERMEDAD) */}
                {formData.tipo === 'POR_ENFERMEDAD' && (
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
                )}

                {/* ✅ Pacientes (solo si es POR_PACIENTES) - CON DARK MODE */}
                {formData.tipo === 'POR_PACIENTES' && (
                <div className="form-group">
                  <label style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem', color: isDarkMode ? '#e2e8f0' : 'inherit' }}>
                    <span>Pacientes * <span style={{ fontSize: '0.85rem', color: '#8b5cf6', fontWeight: '600' }}>({pacientesSeleccionadosTemp.length} seleccionados)</span></span>
                  </label>

                  {/* Barra de búsqueda */}
                  <input
                    type="text"
                    placeholder="Buscar pacientes..."
                    value={pacientesBusqueda}
                    onChange={(e) => setPacientesBusqueda(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '0.5rem 0.75rem',
                      marginBottom: '0.75rem',
                      border: `1px solid ${isDarkMode ? '#475569' : '#e5e7eb'}`,
                      borderRadius: '6px',
                      fontSize: '0.95rem',
                      backgroundColor: isDarkMode ? '#1e293b' : '#fafbfc',
                      color: isDarkMode ? '#e2e8f0' : '#1f2937'
                    }}
                  />

                  {/* Botones de selección rápida */}
                  <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.75rem' }}>
                    <button
                      type="button"
                      onClick={() => setPacientesSeleccionadosTemp(pacientes.map(p => p.id))}
                      style={{
                        flex: 1,
                        padding: '0.4rem 0.75rem',
                        fontSize: '0.85rem',
                        border: `1px solid ${isDarkMode ? '#475569' : '#e5e7eb'}`,
                        borderRadius: '4px',
                        backgroundColor: isDarkMode ? '#0f172a' : '#f0f9ff',
                        color: isDarkMode ? '#38bdf8' : '#0369a1',
                        cursor: 'pointer',
                        fontWeight: '500'
                      }}
                    >
                      Seleccionar todos
                    </button>
                    <button
                      type="button"
                      onClick={() => setPacientesSeleccionadosTemp([])}
                      style={{
                        flex: 1,
                        padding: '0.4rem 0.75rem',
                        fontSize: '0.85rem',
                        border: `1px solid ${isDarkMode ? '#475569' : '#e5e7eb'}`,
                        borderRadius: '4px',
                        backgroundColor: isDarkMode ? '#1e293b' : '#fef2f2',
                        color: isDarkMode ? '#f87171' : '#dc2626',
                        cursor: 'pointer',
                        fontWeight: '500'
                      }}
                    >
                      Limpiar
                    </button>
                  </div>

                  {/* Lista de pacientes filtrada */}
                  <div style={{
                    border: `1px solid ${isDarkMode ? '#475569' : '#e5e7eb'}`,
                    borderRadius: '6px',
                    maxHeight: '300px',
                    overflowY: 'auto',
                    backgroundColor: isDarkMode ? '#1e293b' : '#f9fafb'
                  }}>
                    {pacientes.length === 0 ? (
                      <p style={{ padding: '1rem', color: isDarkMode ? '#64748b' : '#999', marginBottom: 0, textAlign: 'center', fontStyle: 'italic' }}>
                        No hay pacientes disponibles
                      </p>
                    ) : (() => {
                      const pacientesFiltrados = pacientes.filter(p =>
                        `${p.nombre} ${p.apellido}`.toLowerCase().includes(pacientesBusqueda.toLowerCase())
                      );
                      const pacientesAMostrar = mostrarTodosPacientes ? pacientesFiltrados : pacientesFiltrados.slice(0, 6);
                      const hayMas = pacientesFiltrados.length > 6;

                      return (
                        <>
                          {pacientesAMostrar.map(paciente => (
                            <label
                              key={paciente.id}
                              style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: '0.75rem',
                                padding: '0.75rem',
                                borderBottom: `1px solid ${isDarkMode ? '#334155' : '#e5e7eb'}`,
                                cursor: 'pointer',
                                backgroundColor: pacientesSeleccionadosTemp.includes(paciente.id) 
                                  ? isDarkMode ? 'rgba(139, 92, 246, 0.15)' : 'rgba(139, 92, 246, 0.05)'
                                  : 'transparent',
                                transition: 'background-color 0.15s'
                              }}
                              onMouseEnter={(e) => {
                                e.currentTarget.style.backgroundColor = isDarkMode ? 'rgba(139, 92, 246, 0.2)' : 'rgba(139, 92, 246, 0.08)';
                              }}
                              onMouseLeave={(e) => {
                                e.currentTarget.style.backgroundColor = pacientesSeleccionadosTemp.includes(paciente.id) 
                                  ? isDarkMode ? 'rgba(139, 92, 246, 0.15)' : 'rgba(139, 92, 246, 0.05)'
                                  : 'transparent';
                              }}
                            >
                              <input
                                type="checkbox"
                                checked={pacientesSeleccionadosTemp.includes(paciente.id)}
                                onChange={(e) => {
                                  if (e.target.checked) {
                                    setPacientesSeleccionadosTemp([...pacientesSeleccionadosTemp, paciente.id]);
                                  } else {
                                    setPacientesSeleccionadosTemp(pacientesSeleccionadosTemp.filter(id => id !== paciente.id));
                                  }
                                }}
                                style={{ accentColor: '#8b5cf6', cursor: 'pointer' }}
                              />
                              <div style={{ flex: 1 }}>
                                <div style={{ fontWeight: '500', color: isDarkMode ? '#e2e8f0' : '#1f2937' }}>
                                  {paciente.nombre} {paciente.apellido}
                                </div>
                                {paciente.enfermedad && (
                                  <div style={{ fontSize: '0.85rem', color: isDarkMode ? '#94a3b8' : '#6b7280', marginTop: '0.2rem' }}>
                                    {paciente.enfermedad}
                                  </div>
                                )}
                              </div>
                            </label>
                          ))}
                          
                          {/* Ver más / Ver menos */}
                          {hayMas && (
                            <button
                              type="button"
                              onClick={() => setMostrarTodosPacientes(!mostrarTodosPacientes)}
                              style={{
                                width: '100%',
                                padding: '0.75rem',
                                border: 'none',
                                borderTop: `1px solid ${isDarkMode ? '#334155' : '#e5e7eb'}`,
                                backgroundColor: isDarkMode ? '#0f172a' : '#f9fafb',
                                color: '#8b5cf6',
                                cursor: 'pointer',
                                fontWeight: '600',
                                fontSize: '0.9rem',
                                transition: 'background-color 0.15s'
                              }}
                              onMouseEnter={(e) => {
                                e.currentTarget.style.backgroundColor = isDarkMode ? '#1e293b' : '#f3e8ff';
                              }}
                              onMouseLeave={(e) => {
                                e.currentTarget.style.backgroundColor = isDarkMode ? '#0f172a' : '#f9fafb';
                              }}
                            >
                              {mostrarTodosPacientes ? '▲ Ver menos' : `▼ Ver más (${pacientesFiltrados.length - 6})`}
                            </button>
                          )}
                        </>
                      );
                    })()}
                  </div>
                </div>
                )}

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
                  <div className="image-upload-container">
                    <input
                      id="imagen"
                      type="file"
                      accept="image/*"
                      onChange={(e) => {
                        const file = e.target.files?.[0] || null;
                        setImageFile(file);
                        
                        // Crear preview
                        if (file) {
                          const reader = new FileReader();
                          reader.onload = (event) => {
                            setImagePreview(event.target?.result as string);
                          };
                          reader.readAsDataURL(file);
                        } else {
                          setImagePreview(null);
                        }
                      }}
                      className="form-input-file"
                    />
                    
                    {/* Preview o placeholder */}
                    {(imagePreview || (editingId && formData.urlImagen)) ? (
                      <div 
                        className="image-preview-box"
                        onClick={() => document.getElementById('imagen')?.click()}
                      >
                        <img 
                          src={imagePreview || formData.urlImagen} 
                          alt="Preview" 
                          className="image-preview-img"
                        />
                      </div>
                    ) : (
                      <div 
                        className="image-upload-placeholder"
                        onClick={() => document.getElementById('imagen')?.click()}
                      >
                        <p className="placeholder-text">Haz clic para seleccionar imagen</p>
                      </div>
                    )}
                  </div>
                </div>

                {/* Duración */}
                <div className="form-group">
                  <label htmlFor="duracionMinutos">Duración (MM:SS) *</label>
                  <div className="duration-input">
                    <input
                      id="duracionMinutos"
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
                      id="duracionSegundos"
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

          {/* ✅ Warning: No Pacientes Asignados */}
          {showNoPacientesWarning && (
            <ConfirmModal
              isOpen={showNoPacientesWarning}
              title="⚠️ Pacientes No Asignados"
              message="Debes asignar el ejercicio a al menos un paciente antes de guardar. Por favor, selecciona los pacientes en el siguiente paso."
              onConfirm={() => {
                setShowNoPacientesWarning(false);
              }}
              onCancel={() => {
                setShowNoPacientesWarning(false);
              }}
              confirmLabel="Entendido"
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

          {/* Confirm Delete Modal */}
          <ConfirmModal
            isOpen={showConfirmDelete}
            title="¿Eliminar ejercicio?"
            message="¿Estás seguro de que deseas eliminar este ejercicio? Esta acción no se puede deshacer."
            onConfirm={confirmDelete}
            onCancel={() => {
              setShowConfirmDelete(false);
              setDeleteEjercicioId(null);
            }}
            confirmLabel="Eliminar"
            cancelLabel="Cancelar"
            isDarkMode={isDarkMode}
          />

          {/* Detalle Modal */}
          {selectedEjercicioDetalle && (
            <ExerciseDetailsModal
              key={`${selectedEjercicioDetalle.id}-${selectedEjercicioDetalle.pacientesAsignados?.length || 0}`}
              ejercicio={selectedEjercicioDetalle as any}
              pacientes={pacientes}
              isDarkMode={isDarkMode}
              onClose={() => setSelectedEjercicioDetalle(null)}
              onRemoveAssignment={(pacienteId) => {
                handleDesasignarPaciente(selectedEjercicioDetalle.id, pacienteId);
              }}
              onAssignPatients={(pacienteIds) => {
                handleAsignarPacientes(selectedEjercicioDetalle.id, pacienteIds);
              }}
              onDelete={() => handleDelete(selectedEjercicioDetalle.id)}
            />
          )}
        </div>
      </PageTransition>
    </Layout>
  );
};

export default GestionEjercicios;
