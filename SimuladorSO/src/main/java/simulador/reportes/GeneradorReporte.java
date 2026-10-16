package simulador.reportes;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import simulador.modelo.Proceso;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class GeneradorReporte {

    public static void generarReportePDF(String nombreAlgoritmo, List<Proceso> procesos, List<String> historialGantt) {
        try {
            // --- NUEVO: Creación de la carpeta para los reportes ---
            String nombreDirectorio = "Reportes Simulacion";
            File directorio = new File(nombreDirectorio);
            if (!directorio.exists()) {
                directorio.mkdirs(); // Crea la carpeta si no existe
            }
            // -------------------------------------------------------

            // 1. Crear el Gráfico de Barras con JFreeChart
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for (Proceso p : procesos) {
                // Agregamos el Tiempo de Retorno y el Tiempo de Espera por cada proceso
                dataset.addValue(p.getTiempoRetorno(), "Tiempo de Retorno", p.getPid());
                dataset.addValue(p.getTiempoEspera(), "Tiempo de Espera", p.getPid());
            }

            JFreeChart barChart = ChartFactory.createBarChart(
                    "Métricas por Proceso - " + nombreAlgoritmo, // Título del gráfico
                    "Procesos",                                  // Eje X
                    "Ticks (Tiempo)",                            // Eje Y
                    dataset,
                    PlotOrientation.VERTICAL,
                    true, true, false);

            // Guardar el gráfico como imagen PNG temporal (DENTRO DE LA CARPETA)
            String rutaGraficoTemporal = nombreDirectorio + File.separator + "grafico_temp.png";
            ChartUtils.saveChartAsPNG(new File(rutaGraficoTemporal), barChart, 700, 400);

            // 2. Crear el documento PDF con iText (DENTRO DE LA CARPETA)
            String nombreArchivo = nombreDirectorio + File.separator + "Reporte_" + nombreAlgoritmo.replaceAll("[^a-zA-Z0-9]", "_") + ".pdf";
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(nombreArchivo));
            document.open();

            // Título del PDF
            Font fontTitulo = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph titulo = new Paragraph("Reporte de Simulación de CPU", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);

            Font fontSubtitulo = new Font(Font.FontFamily.HELVETICA, 14, Font.NORMAL);
            Paragraph subtitulo = new Paragraph("Algoritmo: " + nombreAlgoritmo, fontSubtitulo);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitulo);
            document.add(new Paragraph("\n"));

            // 3. Crear e insertar la Tabla de Métricas
            PdfPTable table = new PdfPTable(9); // 9 columnas
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2, 2, 2, 3, 3, 2, 2, 2, 2});

            String[] encabezados = {"PID", "Ráfaga", "Llegada", "Comienzo", "Finalización", "Retorno", "Espera", "Páginas", "Faults"};
            Font fontCabecera = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
            Font fontCelda = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);

            for (String header : encabezados) {
                PdfPCell cell = new PdfPCell(new Phrase(header, fontCabecera));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            double sumaRetorno = 0, sumaEspera = 0;

            for (Proceso p : procesos) {
                table.addCell(new Phrase(p.getPid(), fontCelda));
                table.addCell(new Phrase(String.valueOf(p.getRafagaCPU()), fontCelda));
                table.addCell(new Phrase(String.valueOf(p.getTiempoLlegada()), fontCelda));
                table.addCell(new Phrase(p.getTiemposComienzoFormateados(), fontCelda));
                table.addCell(new Phrase(p.getTiemposFinalizacionFormateados(), fontCelda));
                table.addCell(new Phrase(String.valueOf(p.getTiempoRetorno()), fontCelda));
                table.addCell(new Phrase(String.valueOf(p.getTiempoEspera()), fontCelda));
                table.addCell(new Phrase(String.valueOf(p.getCantidadPaginas()), fontCelda));
                table.addCell(new Phrase(String.valueOf(p.getFallosDePagina()), fontCelda));

                sumaRetorno += p.getTiempoRetorno();
                sumaEspera += p.getTiempoEspera();
            }
            document.add(table);
            document.add(new Paragraph("\n"));

            // 4. Promedios
            double promRetorno = sumaRetorno / procesos.size();
            double promEspera = sumaEspera / procesos.size();
            document.add(new Paragraph(String.format("Tiempo Medio de Retorno: %.2f ticks", promRetorno), fontCabecera));
            document.add(new Paragraph(String.format("Tiempo Medio de Espera: %.2f ticks", promEspera), fontCabecera));
            document.add(new Paragraph("\n"));

            // 5. Diagrama de Gantt
            document.add(new Paragraph("Diagrama de Gantt (Línea de tiempo de CPU):", fontCabecera));
            StringBuilder lineaProcesos = new StringBuilder("CPU : ");
            for (String pid : historialGantt) {
                lineaProcesos.append(String.format("[%s]", pid.equals("-") ? "IDLE" : pid));
            }
            Paragraph pGantt = new Paragraph(lineaProcesos.toString(), new Font(Font.FontFamily.COURIER, 9));
            document.add(pGantt);
            document.add(new Paragraph("\n"));

            // 6. Insertar Gráfico Visual en el PDF
            Image imgGrafico = Image.getInstance(rutaGraficoTemporal);
            imgGrafico.setAlignment(Element.ALIGN_CENTER);
            imgGrafico.scaleToFit(500, 350); // Ajusta la imagen al ancho de la hoja
            document.add(imgGrafico);

            // Cerrar el documento
            document.close();

            // Borrar la imagen PNG temporal porque ya quedó embebida en el PDF
            new File(rutaGraficoTemporal).delete();

            System.out.println("[REPORTE] ¡PDF generado exitosamente! Archivo: " + nombreArchivo);

        } catch (Exception e) {
            System.err.println("[ERROR] No se pudo generar el reporte PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }
}