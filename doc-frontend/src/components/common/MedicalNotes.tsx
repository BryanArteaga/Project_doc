import React, { useState, useEffect } from 'react';
import { MessageSquare, Plus, Trash2, Clock, Edit2, X, Check } from 'lucide-react';
import toast from 'react-hot-toast';
import { notaMedicaService, type NotaMedica } from '../../services/notaMedica.service';
import Loading from '../common/Loading';
import { ConfirmModal } from '../shared';
import './MedicalNotes.css';

interface MedicalNotesProps {
  pacienteId: number;
}

const MedicalNotes: React.FC<MedicalNotesProps> = ({ pacienteId }) => {
  const [notas, setNotas] = useState<NotaMedica[]>([]);
  const [loading, setLoading] = useState(true);
  const [newNoteContent, setNewNoteContent] = useState('');
  const [isExpanded, setIsExpanded] = useState(true); // ✅ Iniciar expandido
  const [addingNote, setAddingNote] = useState(false);
  const [isDarkMode, setIsDarkMode] = useState(false);

  // Editar nota
  const [editingNoteId, setEditingNoteId] = useState<number | null>(null);
  const [editingContent, setEditingContent] = useState('');
  const [updatingNote, setUpdatingNote] = useState(false);

  // Confirmar eliminar
  const [deleteConfirm, setDeleteConfirm] = useState<{ isOpen: boolean; notaId: number | null }>({
    isOpen: false,
    notaId: null
  });

  // Detectar dark mode
  useEffect(() => {
    const html = document.documentElement;
    const isDark = html.classList.contains('dark') || html.classList.contains('dark-mode');
    setIsDarkMode(isDark);

    const observer = new MutationObserver(() => {
      const isDark = html.classList.contains('dark') || html.classList.contains('dark-mode');
      setIsDarkMode(isDark);
    });
    observer.observe(html, { attributes: true, attributeFilter: ['class'] });
    return () => observer.disconnect();
  }, []);

  // Cargar notas al montar o cambiar paciente
  useEffect(() => {
    loadNotas();
  }, [pacienteId]);

  const loadNotas = async () => {
    try {
      setLoading(true);
      const data = await notaMedicaService.obtenerPorPaciente(pacienteId);
      setNotas(data);
    } catch (error: any) {
      console.error('Error al cargar notas:', error);
      if (error.response?.status !== 401 && error.response?.status !== 404) {
        toast.error('Error al cargar notas médicas');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleAddNote = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!newNoteContent.trim()) {
      toast.error('La nota no puede estar vacía');
      return;
    }

    try {
      setAddingNote(true);
      const nuevaNota = await notaMedicaService.crear({
        pacienteId,
        contenido: newNoteContent
      });

      setNotas([nuevaNota, ...notas]);
      setNewNoteContent('');
      toast.success('Nota añadida correctamente');
    } catch (error: any) {
      console.error('Error al crear nota:', error);
      toast.error(error.response?.data?.error || 'Error al crear nota');
    } finally {
      setAddingNote(false);
    }
  };

  const handleStartEdit = (nota: NotaMedica) => {
    setEditingNoteId(nota.id);
    setEditingContent(nota.contenido);
  };

  const handleCancelEdit = () => {
    setEditingNoteId(null);
    setEditingContent('');
  };

  const handleSaveEdit = async (notaId: number) => {
    if (!editingContent.trim()) {
      toast.error('La nota no puede estar vacía');
      return;
    }

    try {
      setUpdatingNote(true);
      const notaActualizada = await notaMedicaService.actualizar(notaId, {
        contenido: editingContent
      });

      setNotas(notas.map(n => n.id === notaId ? notaActualizada : n));
      setEditingNoteId(null);
      setEditingContent('');
      toast.success('Nota actualizada correctamente');
    } catch (error: any) {
      console.error('Error al actualizar nota:', error);
      toast.error(error.response?.data?.error || 'Error al actualizar nota');
    } finally {
      setUpdatingNote(false);
    }
  };

  const handleDeleteNote = async (notaId: number) => {
    try {
      console.log('🗑️ Eliminando nota:', notaId);
      await notaMedicaService.eliminar(notaId);
      console.log('✅ Nota eliminada correctamente');
      setNotas(notas.filter(n => n.id !== notaId));
      setDeleteConfirm({ isOpen: false, notaId: null });
      toast.success('Nota eliminada correctamente');
    } catch (error: any) {
      console.error('❌ Error al eliminar nota:', error);
      toast.error(error.response?.data?.error || 'Error al eliminar nota');
    }
  };

  const formatDate = (dateString: string) => {
    try {
      const date = new Date(dateString);
      return new Intl.DateTimeFormat('es-CO', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      }).format(date);
    } catch {
      return dateString;
    }
  };

  return (
    <>
      <div className={`medical-notes-container ${isDarkMode ? 'dark-mode' : ''}`}>
        {/* Header */}
        <div className="medical-notes-header">
          <div className="header-title">
            <MessageSquare size={20} />
            <h3>Observaciones Médicas</h3>
          </div>
          <button
            type="button"
            className="btn-expand"
            onClick={() => setIsExpanded(!isExpanded)}
            title={isExpanded ? 'Contraer' : 'Expandir'}
          >
            {isExpanded ? '−' : '+'}
          </button>
        </div>

        {/* Content */}
        {isExpanded && (
          <div className="medical-notes-content">
            {/* Form para nueva nota */}
            <form onSubmit={handleAddNote} className="medical-notes-form">
              <textarea
                value={newNoteContent}
                onChange={(e) => setNewNoteContent(e.target.value)}
                placeholder="Escribe una observación médica..."
                className="medical-notes-textarea"
                rows={4}
                disabled={addingNote}
              />
              <button
                type="submit"
                className="btn-add-note"
                disabled={addingNote || !newNoteContent.trim()}
              >
                <Plus size={16} />
                {addingNote ? 'Guardando...' : 'Agregar Nota'}
              </button>
            </form>

            {/* Lista de notas */}
            <div className="medical-notes-list">
              {loading ? (
                <div className="loading-state">
                  <Loading message="Cargando notas..." />
                </div>
              ) : notas.length === 0 ? (
                <div className="empty-state">
                  <MessageSquare size={32} />
                  <p>Sin observaciones médicas aún</p>
                </div>
              ) : (
                notas.map((nota) => (
                  <div key={nota.id} className="nota-item">
                    {editingNoteId === nota.id ? (
                      /* Modo edición */
                      <div className="nota-editing">
                        <textarea
                          value={editingContent}
                          onChange={(e) => setEditingContent(e.target.value)}
                          className="nota-textarea-edit"
                          rows={4}
                          disabled={updatingNote}
                        />
                        <div className="nota-edit-actions">
                          <button
                            type="button"
                            className="btn-save-edit"
                            onClick={() => handleSaveEdit(nota.id)}
                            disabled={updatingNote}
                          >
                            <Check size={16} />
                            Guardar
                          </button>
                          <button
                            type="button"
                            className="btn-cancel-edit"
                            onClick={handleCancelEdit}
                            disabled={updatingNote}
                          >
                            <X size={16} />
                            Cancelar
                          </button>
                        </div>
                      </div>
                    ) : (
                      /* Modo vista */
                      <>
                        <div className="nota-header">
                          <div className="nota-info">
                            <span className="nota-doctor">
                              Dr. {nota.doctor?.nombre} {nota.doctor?.apellido}
                            </span>
                            <span className="nota-date">
                              <Clock size={14} />
                              {formatDate(nota.fecha)}
                            </span>
                          </div>
                          <div className="nota-actions">
                            <button
                              type="button"
                              className="btn-edit-note"
                              onClick={() => handleStartEdit(nota)}
                              title="Editar nota"
                            >
                              <Edit2 size={16} />
                            </button>
                            <button
                              type="button"
                              className="btn-delete-note"
                              onClick={() => {
                                console.log('Delete clicked for nota:', nota.id);
                                setDeleteConfirm({ isOpen: true, notaId: nota.id });
                              }}
                              title="Eliminar nota"
                            >
                              <Trash2 size={16} />
                            </button>
                          </div>
                        </div>
                        <div className="nota-content">
                          {nota.contenido}
                        </div>
                      </>
                    )}
                  </div>
                ))
              )}
            </div>
          </div>
        )}

        {/* Collapsed state - mostrar count de notas */}
        {!isExpanded && notas.length > 0 && (
          <div className="medical-notes-collapsed">
            <span className="note-count">
              {notas.length} {notas.length === 1 ? 'nota' : 'notas'}
            </span>
          </div>
        )}
      </div>

      {/* Modal de confirmación de eliminación */}
      <ConfirmModal
        isOpen={deleteConfirm.isOpen}
        title="Eliminar Nota"
        message="¿Estás seguro de que deseas eliminar esta nota? Esta acción no se puede deshacer."
        onConfirm={() => {
          console.log('✅ Confirmar eliminación, notaId:', deleteConfirm.notaId);
          if (deleteConfirm.notaId) {
            handleDeleteNote(deleteConfirm.notaId);
          }
        }}
        onCancel={() => {
          console.log('❌ Cancelar eliminación');
          setDeleteConfirm({ isOpen: false, notaId: null });
        }}
        confirmLabel="Eliminar"
        cancelLabel="Cancelar"
      />
    </>
  );
};

export default MedicalNotes;
