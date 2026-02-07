import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../../components/common/Layout/Layout';
import Input from '../../components/common/Input';
import Loading from '../../components/common/Loading';
import Modal from '../../components/common/Modal';
import { ArrowLeft, Search, UserCheck, UserX, Shield, Edit2, Save, X, CheckCircle } from 'lucide-react';
import toast from 'react-hot-toast';
import type { Doctor } from '../../types';
import api from '../../services/api';
import './ManageDoctors.css';

const ManageDoctors: React.FC = () => {
  const navigate = useNavigate();
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [showSaveConfirmModal, setShowSaveConfirmModal] = useState(false);
  const [showCancelConfirmModal, setShowCancelConfirmModal] = useState(false);
  const [pendingSaveId, setPendingSaveId] = useState<number | null>(null);
  const [editForm, setEditForm] = useState({
    nombre: '',
    apellido: '',
    cedula: '',
    email: '',
    especialidad: '',
    telefono: '',
    activo: true,
    rol: 'DOCTOR' as 'ADMIN' | 'DOCTOR'
  });
  const [originalForm, setOriginalForm] = useState({
    nombre: '',
    apellido: '',
    cedula: '',
    email: '',
    especialidad: '',
    telefono: '',
    activo: true,
    rol: 'DOCTOR' as 'ADMIN' | 'DOCTOR'
  });

  useEffect(() => {
    loadDoctors();
  }, []);

  const loadDoctors = async () => {
    try {
      setLoading(true);
      const response = await api.get<Doctor[]>('/api/doctor/all');
      setDoctors(response.data);
    } catch (error: any) {
      console.error('Error al cargar doctores:', error);
      if (error.response?.status === 401) {
        toast.error('Sesión expirada. Por favor inicia sesión nuevamente.');
      } else {
        toast.error('Error al cargar doctores');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = (doctor: Doctor) => {
    setEditingId(doctor.id);
    const formData = {
      nombre: doctor.nombre,
      apellido: doctor.apellido,
      cedula: doctor.cedula,
      email: doctor.email,
      especialidad: doctor.especialidad || '',
      telefono: doctor.telefono || '',
      activo: doctor.activo,
      rol: doctor.rol
    };
    setEditForm(formData);
    setOriginalForm(formData);
  };

  const hasChanges = () => {
    return (
      editForm.nombre !== originalForm.nombre ||
      editForm.apellido !== originalForm.apellido ||
      editForm.especialidad !== originalForm.especialidad ||
      editForm.telefono !== originalForm.telefono ||
      editForm.activo !== originalForm.activo ||
      editForm.rol !== originalForm.rol
    );
  };

  const handleCancelEdit = () => {
    if (hasChanges()) {
      setShowCancelConfirmModal(true);
    } else {
      cancelEdit();
    }
  };

  const cancelEdit = () => {
    setEditingId(null);
    setEditForm({
      nombre: '',
      apellido: '',
      cedula: '',
      email: '',
      especialidad: '',
      telefono: '',
      activo: true,
      rol: 'DOCTOR'
    });
    setOriginalForm({
      nombre: '',
      apellido: '',
      cedula: '',
      email: '',
      especialidad: '',
      telefono: '',
      activo: true,
      rol: 'DOCTOR'
    });
    setShowCancelConfirmModal(false);
  };

  const handleSaveClick = (doctorId: number) => {
    setPendingSaveId(doctorId);
    setShowSaveConfirmModal(true);
  };

  const confirmSave = async () => {
    if (pendingSaveId === null) return;
    
    try {
      setSaving(true);
      await api.patch(`/api/doctor/${pendingSaveId}`, editForm);
      setShowSaveConfirmModal(false);
      setShowConfirmModal(true);
      setEditingId(null);
      setPendingSaveId(null);
      await loadDoctors();
    } catch (error: any) {
      setShowSaveConfirmModal(false);
      console.error('Error al actualizar doctor:', error);
      if (error.response?.status === 401) {
        toast.error('Sesión expirada. Por favor inicia sesión nuevamente.');
      } else {
        toast.error(error.response?.data?.error || 'Error al actualizar doctor');
      }
    } finally {
      setSaving(false);
    }
  };

  const saveAndExit = async () => {
    if (editingId === null) return;
    
    try {
      setSaving(true);
      setShowCancelConfirmModal(false);
      await api.patch(`/api/doctor/${editingId}`, editForm);
      setShowConfirmModal(true);
      setEditingId(null);
      await loadDoctors();
    } catch (error: any) {
      console.error('Error al actualizar doctor:', error);
      if (error.response?.status === 401) {
        toast.error('Sesión expirada. Por favor inicia sesión nuevamente.');
      } else {
        toast.error(error.response?.data?.error || 'Error al actualizar doctor');
      }
    } finally {
      setSaving(false);
    }
  };

  const handleToggleActive = async (doctorId: number, currentStatus: boolean) => {
    try {
      await api.patch(`/api/doctor/${doctorId}`, { activo: !currentStatus });
      toast.success(currentStatus ? 'Doctor desactivado' : 'Doctor activado');
      loadDoctors();
    } catch (error) {
      toast.error('Error al cambiar estado del doctor');
    }
  };

  const handleToggleRole = async (doctorId: number, currentRole: string) => {
    try {
      const newRole = currentRole === 'ADMIN' ? 'DOCTOR' : 'ADMIN';
      await api.patch(`/api/doctor/${doctorId}`, { rol: newRole });
      toast.success(`Permisos actualizados a ${newRole}`);
      loadDoctors();
    } catch (error) {
      toast.error('Error al actualizar permisos');
    }
  };

  const filteredDoctors = doctors.filter(doctor =>
    doctor.nombre.toLowerCase().includes(searchTerm.toLowerCase()) ||
    doctor.apellido.toLowerCase().includes(searchTerm.toLowerCase()) ||
    doctor.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (doctor.especialidad && doctor.especialidad.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  if (loading) {
    return (
      <Layout>
        <Loading message="Cargando doctores..." fullHeight />
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="manage-doctors-container">
        <div className="manage-doctors-header">
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
            <ArrowLeft style={{ width: '1rem', height: '1rem' }} />
            Volver
          </button>
          <h1 className="manage-doctors-title">Gestionar Doctores</h1>
          <p className="manage-doctors-subtitle">
            Total: {doctors.length} | Admins: {doctors.filter(d => d.rol === 'ADMIN').length} | Doctores: {doctors.filter(d => d.rol === 'DOCTOR').length}
          </p>
        </div>

        {/* Buscador */}
        <div className="search-container">
          <Search className="search-icon" />
          <input
            type="text"
            placeholder="Buscar por nombre, email o especialidad..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="search-input"
          />
        </div>

        {/* Lista de doctores */}
        <div className="doctors-grid">
          {filteredDoctors.length === 0 ? (
            <p className="no-results">No se encontraron doctores</p>
          ) : (
            filteredDoctors.map((doctor) => (
              <div key={doctor.id} className="doctor-card">
                {editingId === doctor.id ? (
                  // Modo edición
                  <div className="doctor-edit-form">
                    <div className="form-row">
                      <Input
                        label="Nombre"
                        value={editForm.nombre}
                        onChange={(e) => setEditForm({ ...editForm, nombre: e.target.value })}
                        placeholder="Ingresa el nombre"
                      />
                      <Input
                        label="Apellido"
                        value={editForm.apellido}
                        onChange={(e) => setEditForm({ ...editForm, apellido: e.target.value })}
                        placeholder="Ingresa el apellido"
                      />
                    </div>
                    <div className="form-row">
                      <Input
                        label="Cédula"
                        value={editForm.cedula}
                        disabled
                        placeholder="No editable"
                      />
                      <Input
                        label="Email"
                        value={editForm.email}
                        disabled
                        placeholder="No editable"
                      />
                    </div>
                    <div className="form-row">
                      <Input
                        label="Especialidad"
                        value={editForm.especialidad}
                        onChange={(e) => setEditForm({ ...editForm, especialidad: e.target.value })}
                        placeholder="Ej: Cardiología, Pediatría, etc."
                      />
                      <Input
                        label="Teléfono"
                        value={editForm.telefono}
                        onChange={(e) => setEditForm({ ...editForm, telefono: e.target.value })}
                        placeholder="Opcional"
                      />
                    </div>
                    <div className="edit-actions">
                      <button
                        onClick={() => handleSaveClick(doctor.id)}
                        style={{
                          backgroundColor: '#16a34a',
                          color: '#ffffff',
                          border: 'none',
                          padding: '0.375rem 0.75rem',
                          borderRadius: '0.5rem',
                          fontWeight: 600,
                          cursor: 'pointer',
                          fontSize: '0.875rem',
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '0.25rem',
                          transition: 'all 0.3s ease'
                        }}
                        onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#15803d'}
                        onMouseLeave={(e) => e.currentTarget.style.backgroundColor = '#16a34a'}
                      >
                        <Save style={{ width: '1rem', height: '1rem' }} />
                        Guardar
                      </button>
                      <button
                        onClick={handleCancelEdit}
                        style={{
                          backgroundColor: '#5B65DC',
                          color: '#ffffff',
                          border: 'none',
                          padding: '0.375rem 0.75rem',
                          borderRadius: '0.5rem',
                          fontWeight: 600,
                          cursor: 'pointer',
                          fontSize: '0.875rem',
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '0.25rem',
                          transition: 'all 0.3s ease'
                        }}
                        onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#4A52C4'}
                        onMouseLeave={(e) => e.currentTarget.style.backgroundColor = '#5B65DC'}
                      >
                        <X style={{ width: '1rem', height: '1rem' }} />
                        Cancelar
                      </button>
                    </div>
                  </div>
                ) : (
                  // Modo vista
                  <>
                    <div className="doctor-card-header">
                      <div className="doctor-avatar">
                        {doctor.nombre.charAt(0)}{doctor.apellido.charAt(0)}
                      </div>
                      <div className="doctor-info">
                        <h3 className="doctor-name">{doctor.nombre} {doctor.apellido}</h3>
                        <p className="doctor-email">{doctor.email}</p>
                        {doctor.especialidad && (
                          <p className="doctor-specialty">🩺 {doctor.especialidad}</p>
                        )}
                        {doctor.cedula && (
                          <p className="doctor-cedula">CI: {doctor.cedula}</p>
                        )}
                      </div>
                    </div>
                    
                    <div className="doctor-card-footer">
                      <div className="doctor-badges">
                        <span className={`doctor-role ${doctor.rol.toLowerCase()}`}>
                          <Shield className="w-4 h-4" />
                          {doctor.rol}
                        </span>
                        <span className={`doctor-status ${doctor.activo ? 'active' : 'inactive'}`}>
                          {doctor.activo ? (
                            <>
                              <UserCheck className="w-4 h-4" />
                              Activo
                            </>
                          ) : (
                            <>
                              <UserX className="w-4 h-4" />
                              Inactivo
                            </>
                          )}
                        </span>
                      </div>
                      <div className="doctor-actions">
                        <button
                          onClick={() => handleEdit(doctor)}
                          style={{
                            backgroundColor: '#5B65DC',
                            color: '#ffffff',
                            border: 'none',
                            padding: '0.375rem 0.75rem',
                            borderRadius: '0.5rem',
                            fontWeight: 600,
                            cursor: 'pointer',
                            fontSize: '0.875rem',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.25rem',
                            transition: 'all 0.3s ease'
                          }}
                          onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#4A52C4'}
                          onMouseLeave={(e) => e.currentTarget.style.backgroundColor = '#5B65DC'}
                        >
                          <Edit2 style={{ width: '1rem', height: '1rem' }} />
                          Editar
                        </button>
                        <button
                          onClick={() => handleToggleRole(doctor.id, doctor.rol)}
                          style={{
                            backgroundColor: doctor.rol === 'ADMIN' ? '#64748b' : '#5B65DC',
                            color: '#ffffff',
                            border: 'none',
                            padding: '0.375rem 0.75rem',
                            borderRadius: '0.5rem',
                            fontWeight: 600,
                            cursor: 'pointer',
                            fontSize: '0.875rem',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.25rem',
                            transition: 'all 0.3s ease'
                          }}
                          onMouseEnter={(e) => {
                            e.currentTarget.style.backgroundColor = doctor.rol === 'ADMIN' ? '#475569' : '#4A52C4';
                          }}
                          onMouseLeave={(e) => {
                            e.currentTarget.style.backgroundColor = doctor.rol === 'ADMIN' ? '#64748b' : '#5B65DC';
                          }}
                        >
                          <Shield style={{ width: '1rem', height: '1rem' }} />
                          {doctor.rol === 'ADMIN' ? 'Quitar Admin' : 'Hacer Admin'}
                        </button>
                        <button
                          onClick={() => handleToggleActive(doctor.id, doctor.activo)}
                          style={{
                            backgroundColor: doctor.activo ? '#dc2626' : '#16a34a',
                            color: '#ffffff',
                            border: 'none',
                            padding: '0.375rem 0.75rem',
                            borderRadius: '0.5rem',
                            fontWeight: 600,
                            cursor: 'pointer',
                            fontSize: '0.875rem',
                            transition: 'all 0.3s ease'
                          }}
                          onMouseEnter={(e) => {
                            e.currentTarget.style.backgroundColor = doctor.activo ? '#b91c1c' : '#15803d';
                          }}
                          onMouseLeave={(e) => {
                            e.currentTarget.style.backgroundColor = doctor.activo ? '#dc2626' : '#16a34a';
                          }}
                        >
                          {doctor.activo ? 'Desactivar' : 'Activar'}
                        </button>
                      </div>
                    </div>
                  </>
                )}
              </div>
            ))
          )}
        </div>

        {/* Modal de confirmación para guardar */}
        <Modal
          isOpen={showSaveConfirmModal}
          onClose={() => !saving && setShowSaveConfirmModal(false)}
          title="Confirmar cambios"
        >
          <div style={{ padding: '1rem 0' }}>
            <p style={{ fontSize: '1rem', marginBottom: '1.5rem', color: '#64748b', textAlign: 'center' }}>
              {saving ? 'Guardando cambios...' : '¿Desea guardar los cambios realizados?'}
            </p>
            {saving ? (
              <div style={{ display: 'flex', justifyContent: 'center' }}>
                <div className="spinner" style={{
                  border: '3px solid #f3f4f6',
                  borderTop: '3px solid #5B65DC',
                  borderRadius: '50%',
                  width: '2rem',
                  height: '2rem',
                  animation: 'spin 0.8s linear infinite'
                }}></div>
              </div>
            ) : (
              <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center' }}>
                <button
                  onClick={confirmSave}
                  style={{
                    backgroundColor: '#16a34a',
                    color: '#ffffff',
                    border: 'none',
                    padding: '0.5rem 1.5rem',
                    borderRadius: '0.5rem',
                    fontWeight: 600,
                    cursor: 'pointer',
                    fontSize: '1rem',
                    transition: 'all 0.3s ease'
                  }}
                  onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#15803d'}
                  onMouseLeave={(e) => e.currentTarget.style.backgroundColor = '#16a34a'}
                >
                  Sí, guardar
                </button>
                <button
                  onClick={() => setShowSaveConfirmModal(false)}
                  style={{
                    backgroundColor: '#64748b',
                    color: '#ffffff',
                    border: 'none',
                    padding: '0.5rem 1.5rem',
                    borderRadius: '0.5rem',
                    fontWeight: 600,
                    cursor: 'pointer',
                    fontSize: '1rem',
                    transition: 'all 0.3s ease'
                  }}
                  onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#475569'}
                  onMouseLeave={(e) => e.currentTarget.style.backgroundColor = '#64748b'}
                >
                  No
                </button>
              </div>
            )}
          </div>
        </Modal>

        {/* Modal de confirmación para cancelar con cambios */}
        <Modal
          isOpen={showCancelConfirmModal}
          onClose={() => !saving && setShowCancelConfirmModal(false)}
          title="Cambios sin guardar"
        >
          <div style={{ padding: '1rem 0' }}>
            <p style={{ fontSize: '1rem', marginBottom: '1.5rem', color: '#64748b', textAlign: 'center' }}>
              {saving ? 'Guardando cambios...' : 'Hay cambios sin guardar. ¿Desea guardar los cambios antes de salir?'}
            </p>
            {saving ? (
              <div style={{ display: 'flex', justifyContent: 'center' }}>
                <div className="spinner" style={{
                  border: '3px solid #f3f4f6',
                  borderTop: '3px solid #5B65DC',
                  borderRadius: '50%',
                  width: '2rem',
                  height: '2rem',
                  animation: 'spin 0.8s linear infinite'
                }}></div>
              </div>
            ) : (
              <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center' }}>
                <button
                  onClick={saveAndExit}
                  style={{
                    backgroundColor: '#16a34a',
                    color: '#ffffff',
                    border: 'none',
                    padding: '0.5rem 1.5rem',
                    borderRadius: '0.5rem',
                    fontWeight: 600,
                    cursor: 'pointer',
                    fontSize: '1rem',
                    transition: 'all 0.3s ease'
                  }}
                  onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#15803d'}
                  onMouseLeave={(e) => e.currentTarget.style.backgroundColor = '#16a34a'}
                >
                  Sí, guardar
                </button>
                <button
                  onClick={cancelEdit}
                  style={{
                    backgroundColor: '#dc2626',
                    color: '#ffffff',
                    border: 'none',
                    padding: '0.5rem 1.5rem',
                    borderRadius: '0.5rem',
                    fontWeight: 600,
                    cursor: 'pointer',
                    fontSize: '1rem',
                    transition: 'all 0.3s ease'
                  }}
                  onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#b91c1c'}
                  onMouseLeave={(e) => e.currentTarget.style.backgroundColor = '#dc2626'}
                >
                  No, descartar
                </button>
              </div>
            )}
          </div>
        </Modal>

        {/* Modal de éxito */}
        <Modal
          isOpen={showConfirmModal}
          onClose={() => setShowConfirmModal(false)}
          title="¡Cambios Guardados!"
        >
          <div style={{ textAlign: 'center', padding: '1rem 0' }}>
            <CheckCircle 
              className="w-16 h-16 mx-auto mb-4" 
              style={{ color: '#22c55e' }}
            />
            <p style={{ fontSize: '1.125rem', marginBottom: '1.5rem', color: '#64748b' }}>
              Los datos del doctor han sido actualizados exitosamente.
            </p>
            <button
              onClick={() => setShowConfirmModal(false)}
              style={{
                backgroundColor: '#5B65DC',
                color: '#ffffff',
                border: 'none',
                padding: '0.5rem 2rem',
                borderRadius: '0.5rem',
                fontWeight: 600,
                cursor: 'pointer',
                fontSize: '1rem',
                transition: 'all 0.3s ease'
              }}
              onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#4A52C4'}
              onMouseLeave={(e) => e.currentTarget.style.backgroundColor = '#5B65DC'}
            >
              Aceptar
            </button>
          </div>
        </Modal>
      </div>
    </Layout>
  );
};

export default ManageDoctors;
