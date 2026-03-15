import React, { ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';
import './CRUDModal.css';

export interface FormError {
  [key: string]: string;
}

export interface CRUDModalProps {
  isOpen: boolean;
  title: string;
  onSubmit: () => void | Promise<void>;
  onCancel: () => void;
  onClose: () => void;
  errors: FormError;
  isLoading?: boolean;
  submitLabel?: string;
  cancelLabel?: string;
  children: ReactNode;
  isDarkMode?: boolean;
}

/**
 * CRUDModal - Modal reutilizable para formularios CRUD
 * Maneja: header, footer con acciones, validación de errores, dark mode
 */
export const CRUDModal: React.FC<CRUDModalProps> = ({
  isOpen,
  title,
  onSubmit,
  onCancel,
  onClose,
  errors,
  isLoading = false,
  submitLabel = 'Guardar',
  cancelLabel = 'Cancelar',
  children,
  isDarkMode = false,
}) => {
  if (!isOpen) return null;

  // Handle ESC key press
  React.useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onCancel();
      }
    };

    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [onCancel]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await onSubmit();
  };

  const containerClass = isDarkMode ? 'crud-modal--dark' : '';

  return createPortal(
    <div
      className={`crud-modal-overlay ${containerClass}`}
      onClick={onCancel}
      role="dialog"
      aria-modal="true"
    >
      <div
        className="crud-modal-container"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="crud-modal-header">
          <h2>{title}</h2>
          <button
            className="crud-modal-close"
            onClick={onCancel}
            type="button"
            aria-label="Cerrar"
          >
            <X size={20} />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="crud-modal-form">
          {children}
        </form>

        {/* Footer */}
        <div className="crud-modal-footer">
          <button
            type="button"
            onClick={onCancel}
            className="crud-modal-btn crud-modal-btn-cancel"
            disabled={isLoading}
          >
            {cancelLabel}
          </button>
          <button
            type="submit"
            onClick={handleSubmit}
            className="crud-modal-btn crud-modal-btn-submit"
            disabled={isLoading || Object.keys(errors).length > 0}
          >
            {isLoading ? 'Guardando...' : submitLabel}
          </button>
        </div>
      </div>
    </div>,
    document.body
  );
};

/**
 * ConfirmModal - Modal de confirmación simple (reutilizado en alerts)
 */
export interface ConfirmModalProps {
  isOpen: boolean;
  title: string;
  message: string;
  onConfirm: () => void | Promise<void>;
  onCancel: () => void;
  confirmLabel?: string;
  cancelLabel?: string;
  isDarkMode?: boolean;
  isLoading?: boolean;
}

export const ConfirmModal: React.FC<ConfirmModalProps> = ({
  isOpen,
  title,
  message,
  onConfirm,
  onCancel,
  confirmLabel = 'Confirmar',
  cancelLabel = 'Cancelar',
  isDarkMode = false,
  isLoading = false,
}) => {
  if (!isOpen) return null;

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') {
      onCancel();
    }
  };

  const containerClass = isDarkMode ? 'crud-modal--dark' : '';

  return createPortal(
    <div
      className={`crud-modal-overlay ${containerClass}`}
      onClick={onCancel}
      onKeyDown={handleKeyDown}
      role="dialog"
      aria-modal="true"
    >
      <div
        className="crud-modal-confirm"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="crud-modal-header">
          <h2>{title}</h2>
        </div>

        <div className="crud-modal-content">
          <p>{message}</p>
        </div>

        <div className="crud-modal-footer">
          <button
            type="button"
            onClick={onCancel}
            className="crud-modal-btn crud-modal-btn-cancel"
            disabled={isLoading}
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            className="crud-modal-btn crud-modal-btn-confirm"
            disabled={isLoading}
          >
            {isLoading ? 'Procesando...' : confirmLabel}
          </button>
        </div>
      </div>
    </div>,
    document.body
  );
};
