import React, { ReactNode } from 'react';
import { Edit2, Trash2 } from 'lucide-react';
import './DataGrid.css';

export interface DataGridCardProps {
  id: string | number;
  title: string;
  description?: string;
  badges?: Array<{
    label: string;
    color?: 'default' | 'success' | 'warning' | 'danger' | 'info';
    className?: string;
    backgroundColor?: string;
    textColor?: string;
  }>;
  onEdit?: () => void;
  onDelete?: () => void;
  onClick?: () => void;
  children?: ReactNode;
  isDarkMode?: boolean;
  customContent?: ReactNode;
  borderLeftColor?: string;
}

/**
 * DataGridCard - Card reutilizable para mostrar items en grid
 */
export const DataGridCard: React.FC<DataGridCardProps> = ({
  id,
  title,
  description,
  badges = [],
  onEdit,
  onDelete,
  onClick,
  children,
  isDarkMode = false,
  customContent,
  borderLeftColor,
}) => {
  const containerClass = `data-grid-card ${isDarkMode ? 'data-grid-card--dark' : ''}`;

  return (
    <div 
      className={containerClass}
      onClick={onClick} 
      role="article"
      style={borderLeftColor ? { borderLeftColor, borderLeftWidth: '4px' } : undefined}
    >
      {/* Header */}
      <div className="data-grid-card-header">
        <h3 className="data-grid-card-title">{title}</h3>
        <div className="data-grid-card-actions">
          {onEdit && (
            <button
              className="data-grid-card-btn data-grid-card-btn-edit"
              onClick={(e) => {
                e.stopPropagation();
                onEdit();
              }}
              title="Editar"
              type="button"
            >
              <Edit2 size={20} />
            </button>
          )}
          {onDelete && (
            <button
              className="data-grid-card-btn data-grid-card-btn-delete"
              onClick={(e) => {
                e.stopPropagation();
                onDelete();
              }}
              title="Eliminar"
              type="button"
            >
              <Trash2 size={20} />
            </button>
          )}
        </div>
      </div>

      {/* Description */}
      {description && (
        <p className="data-grid-card-description">{description}</p>
      )}

      {/* Badges */}
      {badges.length > 0 && (
        <div className="data-grid-card-badges">
          {badges.map((badge, idx) => {
            const hasCustomColor = badge.backgroundColor || badge.textColor;
            const style = hasCustomColor ? {
              backgroundColor: badge.backgroundColor || '#e5e7eb',
              color: badge.textColor || '#1f2937'
            } : undefined;
            
            return (
              <span
                key={idx}
                className={`data-grid-card-badge ${
                  hasCustomColor ? '' : `data-grid-card-badge-${badge.color || 'default'}`
                } ${badge.className || ''}`}
                style={style}
              >
                {badge.label}
              </span>
            );
          })}
        </div>
      )}

      {/* Custom Content */}
      {customContent && <div className="data-grid-card-content">{customContent}</div>}

      {/* Children */}
      {children && <div className="data-grid-card-children">{children}</div>}
    </div>
  );
};

export interface DataGridProps {
  items: DataGridCardProps[];
  emptyMessage?: string;
  isDarkMode?: boolean;
  columns?: number;
  onItemClick?: (item: DataGridCardProps) => void;
}

/**
 * DataGrid - Grid contenedor para cards
 */
export const DataGrid: React.FC<DataGridProps> = ({
  items,
  emptyMessage = 'No hay elementos',
  isDarkMode = false,
  columns = 3,
  onItemClick,
}) => {
  const containerClass = `data-grid ${isDarkMode ? 'data-grid--dark' : ''}`;
  const gridStyle: React.CSSProperties = {
    '--columns': columns,
  } as React.CSSProperties;

  if (items.length === 0) {
    return (
      <div className={`${containerClass} data-grid-empty`}>
        <p>{emptyMessage}</p>
      </div>
    );
  }

  return (
    <div className={containerClass} style={gridStyle}>
      {items.map((item) => (
        <DataGridCard
          key={item.id}
          {...item}
          isDarkMode={isDarkMode}
        />
      ))}
    </div>
  );
};
