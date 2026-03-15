import React from 'react';
import { ArrowUp, ArrowDown } from 'lucide-react';
import './HealthRecordsTable.css';

export interface HealthRecord {
  id: number;
  presionSistolica?: number;
  presionDiastolica?: number;
  glucosa?: number;
  colesterol?: number;
  peso?: number;
  ejercicioMinutos?: number;
  calorias?: number;
  estadoAnimo?: string;
  notas?: string;
  registradoEn: string;
}

interface HealthRecordsTableProps {
  records: HealthRecord[];
  isDarkMode?: boolean;
  onSort?: (column: string) => void;
  sortColumn?: string;
  sortOrder?: 'asc' | 'desc';
}

/**
 * HealthRecordsTable - Tabla de registros de salud con sorting
 * Muestra: presión, glucosa, colesterol, peso, ejercicio, calorías, estado de ánimo
 */
export const HealthRecordsTable: React.FC<HealthRecordsTableProps> = ({
  records,
  isDarkMode = false,
  onSort,
  sortColumn,
  sortOrder = 'asc',
}) => {
  const containerClass = `health-records-table ${isDarkMode ? 'health-records-table--dark' : ''}`;

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

  const emojiEstado = {
    Bueno: '😊',
    Regular: '😐',
    Malo: '😞',
  };

  if (records.length === 0) {
    return (
      <div className={containerClass}>
        <p className="no-records">No hay registros disponibles</p>
      </div>
    );
  }

  return (
    <div className={containerClass}>
      <div className="table-wrapper">
        <table className="health-table">
          <thead>
            <tr>
              <th className="sortable" onClick={() => onSort?.('fecha')}>
                Fecha {renderSortIcon('fecha')}
              </th>
              <th className="sortable" onClick={() => onSort?.('hora')}>
                Hora {renderSortIcon('hora')}
              </th>
              <th className="sortable" onClick={() => onSort?.('presion')}>
                P. Arterial {renderSortIcon('presion')}
              </th>
              <th className="sortable" onClick={() => onSort?.('glucosa')}>
                Glucosa {renderSortIcon('glucosa')}
              </th>
              <th className="sortable" onClick={() => onSort?.('colesterol')}>
                Colesterol {renderSortIcon('colesterol')}
              </th>
              <th className="sortable" onClick={() => onSort?.('peso')}>
                Peso {renderSortIcon('peso')}
              </th>
              <th className="sortable" onClick={() => onSort?.('ejercicio')}>
                Ejercicio {renderSortIcon('ejercicio')}
              </th>
              <th className="sortable" onClick={() => onSort?.('calorias')}>
                Calorías {renderSortIcon('calorias')}
              </th>
              <th className="sortable" onClick={() => onSort?.('estadoAnimo')}>
                Ánimo {renderSortIcon('estadoAnimo')}
              </th>
            </tr>
          </thead>
          <tbody>
            {records.map((record) => (
              <tr key={record.id}>
                <td className="date-cell">{formatDate(record.registradoEn)}</td>
                <td className="time-cell">{formatTime(record.registradoEn)}</td>
                <td className="value-cell">
                  {record.presionSistolica && record.presionDiastolica
                    ? `${record.presionSistolica}/${record.presionDiastolica}`
                    : '-'}
                </td>
                <td className="value-cell">
                  {record.glucosa ? `${record.glucosa} mg/dL` : '-'}
                </td>
                <td className="value-cell">
                  {record.colesterol ? `${record.colesterol} mg/dL` : '-'}
                </td>
                <td className="value-cell">
                  {record.peso ? `${record.peso} kg` : '-'}
                </td>
                <td className="value-cell">
                  {record.ejercicioMinutos ? `${record.ejercicioMinutos} min` : '-'}
                </td>
                <td className="value-cell">
                  {record.calorias ? `${record.calorias} kcal` : '-'}
                </td>
                <td className="emoji-cell">
                  {record.estadoAnimo && emojiEstado[record.estadoAnimo as keyof typeof emojiEstado]}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {records.length > 0 && (
        <p className="records-info">Total: {records.length} registro(s)</p>
      )}
    </div>
  );
};
