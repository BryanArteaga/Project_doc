import React, { useState, useEffect } from 'react';
import Layout from '../../components/common/Layout/Layout';
import PageTransition from '../../components/common/PageTransition';
import { auditService, AuditLevel } from '../../utils/audit';
import type { AuditLog } from '../../utils/audit';
import { Download, RefreshCw, AlertTriangle, Info, AlertCircle, XCircle } from 'lucide-react';
import './AuditLogs.css';

const AuditLogs: React.FC = () => {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [filteredLogs, setFilteredLogs] = useState<AuditLog[]>([]);
  const [filterLevel, setFilterLevel] = useState<string>('ALL');
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    loadLogs();
  }, []);

  useEffect(() => {
    applyFilters();
  }, [logs, filterLevel, searchTerm]);

  const loadLogs = () => {
    const allLogs = auditService.getAllLogs();
    setLogs(allLogs);
  };

  const applyFilters = () => {
    let filtered = [...logs];

    // Filtrar por nivel
    if (filterLevel !== 'ALL') {
      filtered = filtered.filter(log => log.level === filterLevel);
    }

    // Filtrar por búsqueda
    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      filtered = filtered.filter(log =>
        log.action.toLowerCase().includes(term) ||
        log.details?.toLowerCase().includes(term) ||
        log.userName?.toLowerCase().includes(term)
      );
    }

    setFilteredLogs(filtered.reverse());
  };

  const exportLogsCSV = () => {
    const csv = auditService.exportLogsAsCSV();
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `audit-logs-${new Date().toISOString()}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
  };

  const exportLogsJSON = () => {
    const json = auditService.exportLogsAsJSON();
    const blob = new Blob([json], { type: 'application/json' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `audit-logs-${new Date().toISOString()}.json`;
    a.click();
    window.URL.revokeObjectURL(url);
  };

  const clearLogs = () => {
    if (confirm('¿Está seguro de que desea limpiar todos los logs?')) {
      auditService.clearLogs();
      loadLogs();
    }
  };

  const getLevelIcon = (level: AuditLevel) => {
    switch (level) {
      case AuditLevel.INFO:
        return <Info className="w-5 h-5 text-blue-500" />;
      case AuditLevel.WARNING:
        return <AlertTriangle className="w-5 h-5 text-yellow-500" />;
      case AuditLevel.ERROR:
        return <AlertCircle className="w-5 h-5 text-red-500" />;
      case AuditLevel.CRITICAL:
        return <XCircle className="w-5 h-5 text-red-700" />;
    }
  };

  const getLevelClass = (level: AuditLevel) => {
    switch (level) {
      case AuditLevel.INFO:
        return 'audit-level-info';
      case AuditLevel.WARNING:
        return 'audit-level-warning';
      case AuditLevel.ERROR:
        return 'audit-level-error';
      case AuditLevel.CRITICAL:
        return 'audit-level-critical';
    }
  };

  return (
    <Layout>
      <PageTransition>
        <div className="audit-container">
          <div className="audit-header">
            <h1 className="audit-title">Logs de Auditoría</h1>
            <p className="audit-subtitle">
              Monitoreo de actividad y eventos del sistema
            </p>
          </div>

          {/* Controles */}
          <div className="audit-controls">
            <div className="audit-filters">
              <input
                type="text"
                placeholder="Buscar en logs..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="audit-search"
              />
              
              <select
                value={filterLevel}
                onChange={(e) => setFilterLevel(e.target.value)}
                className="audit-select"
              >
                <option value="ALL">Todos los niveles</option>
                <option value="INFO">Info</option>
                <option value="WARNING">Warning</option>
                <option value="ERROR">Error</option>
                <option value="CRITICAL">Critical</option>
              </select>
            </div>

            <div className="audit-actions">
              <button
                onClick={loadLogs}
                className="audit-btn audit-btn-secondary"
                title="Actualizar"
              >
                <RefreshCw className="w-5 h-5" />
              </button>
              <button
                onClick={exportLogsCSV}
                className="audit-btn audit-btn-secondary"
                title="Exportar CSV"
              >
                <Download className="w-5 h-5" />
                CSV
              </button>
              <button
                onClick={exportLogsJSON}
                className="audit-btn audit-btn-secondary"
                title="Exportar JSON"
              >
                <Download className="w-5 h-5" />
                JSON
              </button>
              <button
                onClick={clearLogs}
                className="audit-btn audit-btn-danger"
              >
                Limpiar Logs
              </button>
            </div>
          </div>

          {/* Estadísticas */}
          <div className="audit-stats">
            <div className="audit-stat">
              <span className="audit-stat-label">Total</span>
              <span className="audit-stat-value">{logs.length}</span>
            </div>
            <div className="audit-stat">
              <span className="audit-stat-label">Filtrados</span>
              <span className="audit-stat-value">{filteredLogs.length}</span>
            </div>
            <div className="audit-stat">
              <span className="audit-stat-label">Errores</span>
              <span className="audit-stat-value text-red-500">
                {logs.filter(l => l.level === AuditLevel.ERROR || l.level === AuditLevel.CRITICAL).length}
              </span>
            </div>
          </div>

          {/* Lista de logs */}
          <div className="audit-logs">
            {filteredLogs.length === 0 ? (
              <div className="audit-empty">
                <Info className="w-12 h-12 text-gray-400 mb-2" />
                <p>No hay logs disponibles</p>
              </div>
            ) : (
              filteredLogs.map((log, index) => (
                <div key={index} className={`audit-log-item ${getLevelClass(log.level)}`}>
                  <div className="audit-log-header">
                    <div className="audit-log-icon">
                      {getLevelIcon(log.level)}
                    </div>
                    <div className="audit-log-info">
                      <div className="audit-log-action">{log.action}</div>
                      <div className="audit-log-timestamp">
                        {new Date(log.timestamp).toLocaleString('es-ES')}
                      </div>
                    </div>
                    <div className="audit-log-level">
                      {log.level}
                    </div>
                  </div>
                  
                  {log.details && (
                    <div className="audit-log-details">
                      {log.details}
                    </div>
                  )}
                  
                  {log.userName && (
                    <div className="audit-log-user">
                      <strong>Usuario:</strong> {log.userName}
                      {log.userRole && <span className="audit-log-role">({log.userRole})</span>}
                    </div>
                  )}
                  
                  {log.metadata && Object.keys(log.metadata).length > 0 && (
                    <div className="audit-log-metadata">
                      <details>
                        <summary>Metadata</summary>
                        <pre>{JSON.stringify(log.metadata, null, 2)}</pre>
                      </details>
                    </div>
                  )}
                </div>
              ))
            )}
          </div>
        </div>
      </PageTransition>
    </Layout>
  );
};

export default AuditLogs;
