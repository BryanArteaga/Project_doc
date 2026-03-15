import React from 'react';
import { ArrowUp, ArrowDown } from 'lucide-react';
import './ExerciseHistoryTable.css';

export interface ExerciseRecord {
  id: number;
  completadoEn: string;
  ejercicio: {
    nombre: string;
    descripcion?: string;
    duracion?: number;
  };
}

interface ExerciseHistoryTableProps {
  exercises: ExerciseRecord[];
  isDarkMode?: boolean;
  onSort?: (column: string) => void;
  sortColumn?: string;
  sortOrder?: 'asc' | 'desc';
}

/**
 * ExerciseHistoryTable - Tabla de historial de ejercicios realizados
 */
export const ExerciseHistoryTable: React.FC<ExerciseHistoryTableProps> = ({
  exercises,
  isDarkMode = false,
  onSort,
  sortColumn,
  sortOrder = 'asc',
}) => {
  const containerClass = `exercise-history-table ${isDarkMode ? 'exercise-history-table--dark' : ''}`;

  const formatDate = (date: string) => {
    return new Date(date).toLocaleDateString('es-ES', {
      year: '2-digit',
      month: '2-digit',
      day: '2-digit',
    });
  };

  const formatTime = (date: string) => {
    return new Date(date).toLocaleTimeString('es-ES', {
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const renderSortIcon = (column: string) => {
    if (sortColumn !== column) return null;
    return sortOrder === 'asc' ? (
      <ArrowUp size={16} className="sort-icon" />
    ) : (
      <ArrowDown size={16} className="sort-icon" />
    );
  };

  if (exercises.length === 0) {
    return (
      <div className={containerClass}>
        <p className="no-exercises">No hay ejercicios realizados</p>
      </div>
    );
  }

  return (
    <div className={containerClass}>
      <div className="table-wrapper">
        <table className="exercises-table">
          <thead>
            <tr>
              <th className="sortable" onClick={() => onSort?.('ejercicio-id')}>
                ID {renderSortIcon('ejercicio-id')}
              </th>
              <th className="sortable" onClick={() => onSort?.('ejercicio-nombre')}>
                Nombre {renderSortIcon('ejercicio-nombre')}
              </th>
              <th className="sortable" onClick={() => onSort?.('ejercicio-fecha')}>
                Fecha {renderSortIcon('ejercicio-fecha')}
              </th>
              <th className="sortable" onClick={() => onSort?.('ejercicio-hora')}>
                Hora {renderSortIcon('ejercicio-hora')}
              </th>
              <th>Duración</th>
              <th>Descripción</th>
            </tr>
          </thead>
          <tbody>
            {exercises.map((exercise) => (
              <tr key={exercise.id}>
                <td className="id-cell">{exercise.id}</td>
                <td className="name-cell">{exercise.ejercicio.nombre}</td>
                <td className="date-cell">{formatDate(exercise.completadoEn)}</td>
                <td className="time-cell">{formatTime(exercise.completadoEn)}</td>
                <td className="duration-cell">
                  {exercise.ejercicio.duracion ? `${exercise.ejercicio.duracion} min` : '-'}
                </td>
                <td className="description-cell" title={exercise.ejercicio.descripcion}>
                  {exercise.ejercicio.descripcion || '-'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {exercises.length > 0 && (
        <p className="exercises-info">Total: {exercises.length} ejercicio(s)</p>
      )}
    </div>
  );
};
