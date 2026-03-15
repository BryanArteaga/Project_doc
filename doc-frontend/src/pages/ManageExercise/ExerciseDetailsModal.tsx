import React, { useState, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { X, Trash2, Search } from 'lucide-react';
import './ExerciseDetailsModal.css';

interface Paciente {
  id: number;
  nombre: string;
  apellido: string;
  enfermedad: string;
  cedula?: string;
}

interface PacienteAsignado extends Paciente {}

interface Ejercicio {
  id: number;
  nombre: string;
  descripcion?: string;
  enfermedad: string;
  intensidad: string;
  duracion: number;
  series: number;
  descansoEntreSeries: number;
  totalAsignados?: number;
  pacientesAsignados?: PacienteAsignado[];
}

interface ExerciseDetailsModalProps {
  onClose: () => void;
  ejercicio: Ejercicio;
  pacientes: Paciente[];
  isDarkMode?: boolean;
  onRemoveAssignment?: (pacienteId: number) => void;
  onConfirmClose?: () => void;
}

type ModalMode = 'details' | 'assign' | 'assigned';

const ExerciseDetailsModal: React.FC<ExerciseDetailsModalProps> = ({
  onClose,
  ejercicio,
  pacientes,
  isDarkMode = false,
  onRemoveAssignment,
  onConfirmClose,
}) => {
  // Modal modes
  const [mode, setMode] = useState<ModalMode>('details');
  const [showConfirmClose, setShowConfirmClose] = useState(false);
  const [isClosing, setIsClosing] = useState(false);
  
  // Assignment search & selection
  const [searchPaciente, setSearchPaciente] = useState('');
  const [filterEnfermedad, setFilterEnfermedad] = useState('');
  const [selectedPacientes, setSelectedPacientes] = useState<number[]>([]);

  // Handle ESC key press
  React.useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        if (showConfirmClose) {
          setShowConfirmClose(false);
        } else {
          handleClose();
        }
      }
    };

    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [showConfirmClose]);

  // Prevent body scroll
  React.useEffect(() => {
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = 'unset';
    };
  }, []);

  // Get unique enfermedades from pacientes
  const enfermedadesOptions = useMemo(() => {
    const unique = new Set(pacientes.map(p => p.enfermedad));
    return Array.from(unique).sort();
  }, [pacientes]);

  // Filter pacientes based on search and filters
  const filteredPacientes = useMemo(() => {
    return pacientes.filter(p => {
      const matchSearch = 
        p.nombre.toLowerCase().includes(searchPaciente.toLowerCase()) ||
        p.apellido.toLowerCase().includes(searchPaciente.toLowerCase()) ||
        p.cedula?.includes(searchPaciente);
      
      const matchEnfermedad = !filterEnfermedad || p.enfermedad === filterEnfermedad;
      
      // Exclude already assigned patients
      const isNotAssigned = !ejercicio.pacientesAsignados?.some(pa => pa.id === p.id);
      
      return matchSearch && matchEnfermedad && isNotAssigned;
    });
  }, [searchPaciente, filterEnfermedad, pacientes, ejercicio.pacientesAsignados]);

  // Toggle paciente selection
  const togglePacienteSelection = (pacienteId: number) => {
    setSelectedPacientes(prev => {
      if (prev.includes(pacienteId)) {
        return prev.filter(id => id !== pacienteId);
      } else {
        return [...prev, pacienteId];
      }
    });
  };

  // Switch between modes
  const switchMode = (newMode: ModalMode) => {
    setMode(newMode);
    if (newMode !== 'assign') {
      setSelectedPacientes([]);
      setSearchPaciente('');
      setFilterEnfermedad('');
    }
  };

  // Handle close with confirmation if there are unsaved changes
  const handleClose = () => {
    if (mode === 'assign' && selectedPacientes.length > 0) {
      setShowConfirmClose(true);
    } else {
      confirmClose();
    }
  };

  // Confirm and execute close
  const confirmClose = async () => {
    setIsClosing(true);
    try {
      if (onConfirmClose) {
        await onConfirmClose();
      }
      setShowConfirmClose(false);
      onClose();
    } finally {
      setIsClosing(false);
    }
  };

  return createPortal(
    <div className="exercise-modal-overlay" onClick={handleClose}>
      <div className={`exercise-details-modal ${isDarkMode ? 'dark' : ''}`} onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="modal-header">
          <h2>{ejercicio.nombre}</h2>
          <button onClick={handleClose} className="modal-close" type="button" aria-label="Cerrar">
            <X size={20} />
          </button>
        </div>

        {/* Content */}
        <div className="modal-body">
          {/* Mode: Details */}
          {mode === 'details' && (
            <>
              {/* Header with Image and Title */}
              <div className="details-header">
                {ejercicio.urlImagen && (
                  <div className="header-image">
                    <img 
                      src={ejercicio.urlImagen} 
                      alt={ejercicio.nombre}
                      className="exercise-image"
                    />
                  </div>
                )}
                <div className="header-content">
                  <h2>{ejercicio.nombre}</h2>
                </div>
              </div>

              {/* Basic Details Section */}
              <div className="details-section">
                {/* Enfermedad */}
                <div className="detail-row">
                  <span className="detail-label">Enfermedad:</span>
                  <p className="detail-value">{ejercicio.enfermedad}</p>
                </div>

                {/* Descripción */}
                <div className="detail-row">
                  <span className="detail-label">Descripción:</span>
                  <p className="detail-value description">{ejercicio.descripcion || '-'}</p>
                </div>

                {/* Grid 2 Columnas */}
                <div className="details-grid-2col">
                  <div className="detail-item">
                    <span className="detail-label">Intensidad:</span>
                    <p className="detail-value">{ejercicio.intensidad}</p>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">Duración:</span>
                    <p className="detail-value">{Math.floor(ejercicio.duracion / 60)}:{(ejercicio.duracion % 60).toString().padStart(2, '0')} min</p>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">Series:</span>
                    <p className="detail-value">{ejercicio.series}</p>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">Descanso entre series:</span>
                    <p className="detail-value">{ejercicio.descansoEntreSeries}s</p>
                  </div>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="action-buttons">
                <button
                  onClick={() => switchMode('assign')}
                  className="btn-action btn-assign"
                  type="button"
                >
                  Asignar a Paciente
                </button>
                <button
                  onClick={() => switchMode('assigned')}
                  className="btn-action btn-assigned"
                  type="button"
                >
                  Pacientes Asignados ({ejercicio.totalAsignados || 0})
                </button>
              </div>
            </>
          )}

          {/* Mode: Assign */}
          {mode === 'assign' && (
            <>
              <div className="search-section">
                <h3>Asignar Pacientes</h3>
                
                {/* Search Bar */}
                <div className="search-input-wrapper">
                  <Search size={18} className="search-icon" />
                  <input
                    type="text"
                    placeholder="Buscar por nombre o cédula..."
                    value={searchPaciente}
                    onChange={(e) => setSearchPaciente(e.target.value)}
                    className="search-input"
                  />
                </div>

                {/* Filters */}
                <div className="filters-wrapper">
                  <select
                    value={filterEnfermedad}
                    onChange={(e) => setFilterEnfermedad(e.target.value)}
                    className="filter-select"
                  >
                    <option value="">Todas las enfermedades</option>
                    {enfermedadesOptions.map(enf => (
                      <option key={enf} value={enf}>{enf}</option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Pacientes List with Checkboxes */}
              <div className="pacientes-selection">
                {filteredPacientes.length > 0 ? (
                  <div className="pacientes-items">
                    {filteredPacientes.map(paciente => (
                      <label key={paciente.id} className="paciente-checkbox-item">
                        <input
                          type="checkbox"
                          checked={selectedPacientes.includes(paciente.id)}
                          onChange={() => togglePacienteSelection(paciente.id)}
                          className="checkbox-input"
                        />
                        <div className="paciente-info">
                          <span className="paciente-nombre">{paciente.nombre} {paciente.apellido}</span>
                          <span className="paciente-enfermedad">{paciente.enfermedad}</span>
                          {paciente.cedula && <span className="paciente-cedula">{paciente.cedula}</span>}
                        </div>
                      </label>
                    ))}
                  </div>
                ) : (
                  <div className="no-results">
                    {pacientes.length === 0 ? 'No hay pacientes disponibles' : 'No se encontraron pacientes'}
                  </div>
                )}
              </div>

              {/* Assignment Buttons */}
              <div className="assignment-buttons">
                <button
                  onClick={() => switchMode('details')}
                  className="btn-cancel"
                  type="button"
                >
                  Cancelar
                </button>
                <button
                  onClick={() => {
                    // TODO: Agregar lógica de asignación
                    console.log('Asignar pacientes:', selectedPacientes);
                    switchMode('details');
                  }}
                  className="btn-assign-submit"
                  disabled={selectedPacientes.length === 0}
                  type="button"
                >
                  Asignar Ejercicio ({selectedPacientes.length})
                </button>
              </div>
            </>
          )}

          {/* Mode: Assigned */}
          {mode === 'assigned' && (
            <>
              <div className="assigned-section">
                <h3>Pacientes Asignados ({ejercicio.totalAsignados || 0})</h3>
                
                {ejercicio.pacientesAsignados && ejercicio.pacientesAsignados.length > 0 ? (
                  <div className="assigned-items">
                    {ejercicio.pacientesAsignados.map(paciente => (
                      <div key={paciente.id} className="assigned-item">
                        <div className="assigned-info">
                          <span className="assigned-nombre">{paciente.nombre} {paciente.apellido}</span>
                          <span className="assigned-enfermedad">{paciente.enfermedad}</span>
                        </div>
                        <button
                          onClick={() => {
                            if (onRemoveAssignment) {
                              onRemoveAssignment(paciente.id);
                            }
                          }}
                          className="btn-remove"
                          type="button"
                          title="Remover asignación"
                        >
                          <Trash2 size={18} />
                        </button>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="no-assigned">
                    No hay pacientes asignados a este ejercicio
                  </div>
                )}
              </div>

              {/* Back Button */}
              <div className="back-button-wrapper">
                <button
                  onClick={() => switchMode('details')}
                  className="btn-back"
                  type="button"
                >
                  Volver
                </button>
              </div>
            </>
          )}
        </div>

        {/* Confirm Close Modal */}
        {showConfirmClose && (
          <div className="exercise-modal-overlay" onClick={() => !isClosing && setShowConfirmClose(false)}>
            <div className="exercise-details-modal dark" onClick={(e) => e.stopPropagation()}>
              <div className="modal-header">
                <h2>Descartar cambios</h2>
                <button onClick={() => !isClosing && setShowConfirmClose(false)} className="modal-close" type="button">
                  <X size={20} />
                </button>
              </div>
              <div className="modal-body" style={{ paddingTop: '1rem', paddingBottom: '1rem' }}>
                <p style={{ fontSize: '1rem', marginBottom: '1.5rem', color: '#cbd5e1', textAlign: 'center' }}>
                  Tienes pacientes seleccionados para asignar. ¿Deseas cerrar sin asignarlos?
                </p>
                <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center' }}>
                  <button
                    onClick={confirmClose}
                    disabled={isClosing}
                    style={{
                      backgroundColor: '#ef4444',
                      color: '#ffffff',
                      border: 'none',
                      padding: '0.5rem 1.5rem',
                      borderRadius: '0.5rem',
                      fontWeight: 600,
                      cursor: isClosing ? 'not-allowed' : 'pointer',
                      fontSize: '1rem',
                      transition: 'all 0.3s ease',
                      opacity: isClosing ? 0.6 : 1,
                    }}
                    onMouseEnter={(e) => !isClosing && (e.currentTarget.style.backgroundColor = '#dc2626')}
                    onMouseLeave={(e) => !isClosing && (e.currentTarget.style.backgroundColor = '#ef4444')}
                    type="button"
                  >
                    Sí, descartar
                  </button>
                  <button
                    onClick={() => setShowConfirmClose(false)}
                    disabled={isClosing}
                    style={{
                      backgroundColor: '#64748b',
                      color: '#ffffff',
                      border: 'none',
                      padding: '0.5rem 1.5rem',
                      borderRadius: '0.5rem',
                      fontWeight: 600,
                      cursor: isClosing ? 'not-allowed' : 'pointer',
                      fontSize: '1rem',
                      transition: 'all 0.3s ease',
                      opacity: isClosing ? 0.6 : 1,
                    }}
                    onMouseEnter={(e) => !isClosing && (e.currentTarget.style.backgroundColor = '#475569')}
                    onMouseLeave={(e) => !isClosing && (e.currentTarget.style.backgroundColor = '#64748b')}
                    type="button"
                  >
                    No, continuar
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>,
    document.body
  );
};

export default ExerciseDetailsModal;
