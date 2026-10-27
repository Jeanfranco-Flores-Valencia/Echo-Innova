/**
 * Echo Guard · Receptor de opiniones en Google Sheets (opcional).
 *
 * 1. Crea una hoja de cálculo en Google Drive → Extensiones → Apps Script.
 * 2. Pega este código y guarda.
 * 3. Implementar → Nueva implementación → Tipo: Aplicación web.
 *    Ejecutar como: tú · Quién tiene acceso: Cualquier persona.
 * 4. Copia la URL (termina en /exec) y agrégala a local.properties:
 *      feedbackUrl=https://script.google.com/macros/s/XXXX/exec
 * 5. Vuelve a compilar la app. Cada opinión llegará como una fila nueva;
 *    si no hay internet, la app la guarda y la envía después.
 */
function doPost(e) {
  var data = JSON.parse(e.postData.contents);
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName('Opiniones')
    || SpreadsheetApp.getActiveSpreadsheet().insertSheet('Opiniones');
  if (sheet.getLastRow() === 0) {
    sheet.appendRow(['recibido', 'id', 'fecha', 'nombre', 'rol', 'aula', 'facilidad', 'claridad', 'confianza',
      'diseño', 'recomendaria_0_10', 'tareas', 'le_gusto', 'mejoras', 'errores', 'version', 'dispositivo']);
  }
  var tasks = (data.tasks || []).map(function (t) { return t.task + ':' + t.outcome; }).join(' | ');
  sheet.appendRow([new Date(), data.id, new Date(data.createdAt), data.userName, data.role, data.classroomId || '',
    data.ease, data.clarity, data.confidence, data.design, data.recommend, tasks,
    data.liked, data.improvements, data.bugs, data.appVersion, data.device]);
  return ContentService.createTextOutput(JSON.stringify({ ok: true })).setMimeType(ContentService.MimeType.JSON);
}
