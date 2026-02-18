/**
 * @file svg.ts
 * @description Типы для SVG-элементов, используемых в нативном модуле.
 *              Types for SVG elements used by the native module.
 *
 * Нативный Android-вид принимает иконки в виде массива SVG-примитивов,
 * который сериализуется через Expo Modules Bridge.
 *
 * The native Android view accepts icons as an array of SVG primitives
 * serialized through the Expo Modules Bridge.
 */

/**
 * Описание одного SVG-элемента для передачи в нативный слой.
 * Поддерживаемые типы: "path", "circle", "line", "polyline", "polygon", "rect".
 *
 * Descriptor of a single SVG element passed to the native layer.
 * Supported types: "path", "circle", "line", "polyline", "polygon", "rect".
 */
export interface SvgElement {
  /**
   * Тип SVG-элемента.
   * SVG element type.
   */
  type: string;

  // --- path ---

  /**
   * Данные SVG-пути (только для type === "path").
   * SVG path data (only for type === "path").
   */
  d?: string;

  // --- circle ---

  /**
   * Координата X центра окружности.
   * X coordinate of the circle center.
   */
  cx?: string;

  /**
   * Координата Y центра окружности.
   * Y coordinate of the circle center.
   */
  cy?: string;

  /**
   * Радиус окружности.
   * Circle radius.
   */
  r?: string;

  // --- line ---

  /**
   * Начальная координата X линии.
   * Line start X coordinate.
   */
  x1?: string;

  /**
   * Начальная координата Y линии.
   * Line start Y coordinate.
   */
  y1?: string;

  /**
   * Конечная координата X линии.
   * Line end X coordinate.
   */
  x2?: string;

  /**
   * Конечная координата Y линии.
   * Line end Y coordinate.
   */
  y2?: string;

  // --- polyline / polygon ---

  /**
   * Точки ломаной или многоугольника в формате "x1,y1 x2,y2 ...".
   * Polyline or polygon points in "x1,y1 x2,y2 ..." format.
   */
  points?: string;

  // --- rect ---

  /**
   * Левый край прямоугольника.
   * Left edge of the rectangle.
   */
  x?: string;

  /**
   * Верхний край прямоугольника.
   * Top edge of the rectangle.
   */
  y?: string;

  /**
   * Ширина прямоугольника.
   * Rectangle width.
   */
  width?: string;

  /**
   * Высота прямоугольника.
   * Rectangle height.
   */
  height?: string;

  /**
   * Радиус скругления углов по оси X.
   * X-axis corner radius.
   */
  rx?: string;

  /**
   * Радиус скругления углов по оси Y.
   * Y-axis corner radius.
   */
  ry?: string;
}
