import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import './Pagination.css';

export interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  itemsPerPage?: number;
  totalItems?: number;
  isDarkMode?: boolean;
  isDisabled?: boolean;
}

/**
 * Pagination - Componente reutilizable de paginación
 * Maneja: navegación de páginas, botones anterior/siguiente, números dinámicos
 */
export const Pagination: React.FC<PaginationProps> = ({
  currentPage,
  totalPages,
  onPageChange,
  itemsPerPage = 10,
  totalItems,
  isDarkMode = false,
  isDisabled = false,
}) => {
  if (totalPages <= 1) return null;

  const getPageNumbers = (): (number | string)[] => {
    const pages: (number | string)[] = [];

    if (totalPages <= 4) {
      // Si hay 4 o menos páginas, mostrarlas todas
      for (let i = 1; i <= totalPages; i++) {
        pages.push(i);
      }
    } else {
      // Lógica compleja para mostrar máximo 4 botones
      if (currentPage <= 3) {
        // Al inicio: mostrar [1, 2, 3, 4]
        pages.push(1, 2, 3, 4);
      } else if (currentPage >= totalPages - 2) {
        // Al final: mostrar últimas 4
        pages.push(totalPages - 3, totalPages - 2, totalPages - 1, totalPages);
      } else {
        // En el medio: mostrar [current-1, current, current+1, current+2]
        pages.push(
          currentPage - 1,
          currentPage,
          currentPage + 1,
          currentPage + 2
        );
      }
    }

    return pages;
  };

  const pageNumbers = getPageNumbers();
  const containerClass = `pagination ${isDarkMode ? 'pagination--dark' : ''}`;

  const handlePrevious = () => {
    if (currentPage > 1 && !isDisabled) {
      onPageChange(currentPage - 1);
    }
  };

  const handleNext = () => {
    if (currentPage < totalPages && !isDisabled) {
      onPageChange(currentPage + 1);
    }
  };

  const handlePageClick = (page: number) => {
    if (!isDisabled) {
      onPageChange(page);
    }
  };

  return (
    <div className={containerClass}>
      {/* Info Text */}
      {totalItems && (
        <span className="pagination-info">
          Página {currentPage} de {totalPages}
          {totalItems && ` (${totalItems} items)`}
        </span>
      )}

      {/* Navigation */}
      <div className="pagination-controls">
        {/* Previous Button */}
        <button
          onClick={handlePrevious}
          disabled={currentPage === 1 || isDisabled}
          className="pagination-arrow pagination-prev"
          aria-label="Página anterior"
        >
          <ChevronLeft size={18} />
        </button>

        {/* Page Numbers */}
        {pageNumbers.map((page, idx) =>
          typeof page === 'number' ? (
            <button
              key={idx}
              onClick={() => handlePageClick(page)}
              disabled={isDisabled}
              className={`pagination-number ${
                page === currentPage ? 'active' : ''
              }`}
              aria-label={`Ir a página ${page}`}
              aria-current={page === currentPage ? 'page' : undefined}
            >
              {page}
            </button>
          ) : null
        )}

        {/* Next Button */}
        <button
          onClick={handleNext}
          disabled={currentPage === totalPages || isDisabled}
          className="pagination-arrow pagination-next"
          aria-label="Próxima página"
        >
          <ChevronRight size={18} />
        </button>
      </div>
    </div>
  );
};
