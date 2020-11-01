import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * @author Gonzalo Bueno Rodriguez
 */
public class FingerPrintImage {
    Scanner opcion;
    BufferedImage imagenOriginal;//para asignarla al principio al elegir archivo, arrastramos en todos los procesos su tamaño, asi todas las imagenes seran del tamaño de la inicial
    int umbral;
    int anchuraOriginal;
    int alturaOriginal;

    /**
     * constructor por defecto
     */
    public FingerPrintImage() {
        this.opcion = new Scanner(System.in);//scanner para la entrada de opciones
        this.imagenOriginal = null;
        this.umbral = 50;//valor por defecto de umbral a 50
        this.anchuraOriginal = 0;
        this.alturaOriginal = 0;
    }

    /**
     * metodo para seleccionar la imagen original a usar, debemos leer del fichero y pasarla a BufferedImage para tratarla
     *
     * @param imagenSeleccionada con el nombre de la imagen seleccionada en la consola
     * @return nada
     * @throws IOException
     */
    private void seleccionarArchivo(String imagenSeleccionada) throws IOException {
        imagenOriginal = ImageIO.read(new File(imagenSeleccionada));//leemos la imagen seleccionada por el usuario
        System.out.println("\nHa seleccionado la imagen " + imagenSeleccionada + "\n");
        this.anchuraOriginal = imagenOriginal.getWidth();//obtenemos ancho y alto de la imagen original para futuras imagenes
        this.alturaOriginal = imagenOriginal.getHeight();
    }

    /**
     * metodo para pasar de matriz a BufferedImage rgb y a un archivo para su visualizacion
     *
     * @param imagenEntrada con la matriz de entrada,
     * @param nombreArchivo con el nombre del nuevo archivo
     * @param modo          con el modo, 1 grises, 0 blanco y negro
     * @return nada
     * @throws IOException
     */
    private void pasarImagenAArchivo(int[][] imagenEntrada, String nombreArchivo, int modo) throws IOException {
        BufferedImage imagenRGB = new BufferedImage(anchuraOriginal, alturaOriginal, BufferedImage.TYPE_INT_RGB);
        imagenRGB = convertirImagenARGB(imagenEntrada, modo);//pasamos primero a rgb
        ImageIO.write(imagenRGB, "jpg", new File(nombreArchivo));//creamos un archivo nuevo para su comprovacion visual
    }

    /**
     * metodo para convertir la imagen inicial a una matriz de enteros de grises de o a 255, es el primer paso
     *
     * @param imagenEntrada con la imagen de entrada de tipo BufferedImage
     * @return imagenSalida con la imagen pasada a matriz en escala de grises
     */
    private int[][] convertirImagenAGrises(BufferedImage imagenEntrada) {
        int imagenSalida[][] = new int[anchuraOriginal][alturaOriginal];
        for (int x = 0; x < imagenEntrada.getWidth(); x++) {//recorrido imagen
            for (int y = 0; y < imagenEntrada.getHeight(); y++) {
                int rgb = imagenEntrada.getRGB(x, y);
                int R = (rgb >> 16) & 0xFF;//obtenemos rojo verde y azul de cada pixel y hacemos AND con negro
                int G = (rgb >> 8) & 0xFF;
                int B = (rgb & 0xFF);
                int nivelGris = (R + G + B) / 3;
//                int nivelGris = (0,2126*R) + (0,7152*G) + (0,0722*B); //otra forma de calcular nivelGris
                imagenSalida[x][y] = nivelGris;//no es necesario setPixel, lo asignamos directamente a la matriz el color, que es un entero, a nuestro pixel
            }
        }
        return imagenSalida;//esta matriz ya sera del ancho y alto que la imagen original en pixeles, ya la hemos pasado a matriz, la arrastraremos para los demas pasos
    }

    /**
     * metodo para convertir matriz a BufferedImage rgb para crear despues un archivo y mostrarla
     *
     * @param imagenEntrada con la matriz de entrada
     * @param modo          un entero para saber si estamos pasando una imagen a RGB en grises o en blanco y negro
     *                      modo=0 si la matriz esta binarizada, 1 si no lo esta
     * @return imagenRGB con la imagen pasada a BufferedImage
     */
    private BufferedImage convertirImagenARGB(int[][] imagenEntrada, int modo) {
        BufferedImage imagenRGB = new BufferedImage(anchuraOriginal, alturaOriginal, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < imagenEntrada.length; x++) {//no es necesario getWidth y getHeight, ya que es una matriz, nos ahorramos esos metodos de FingerPrintImage
            for (int y = 0; y < imagenEntrada[0].length; y++) {
                int valor;
                valor = imagenEntrada[x][y];
                if (modo == 0) {
                    valor = valor * 255;
                }
                int pixelRGB = (255 << 24 | valor << 16 | valor << 8 | valor);
                imagenRGB.setRGB(x, y, pixelRGB);
            }
        }
        return imagenRGB;
    }

    /**
     * metodo para ecualizar la imagen para aumentar el contraste mediante un histograma
     *
     * @param imagenEntrada con la matriz de entrada
     * @return imagenSalida con la matriz ecualizada
     */
    private int[][] calcularHistograma(int[][] imagenEntrada) {
        int[][] imagenEcualizada = new int[anchuraOriginal][alturaOriginal];
        int ancho = imagenEntrada.length;// getWidth
        int alto = imagenEntrada[0].length;// getHeight
        int tampixel = ancho * alto;
        int[] histograma = new int[256];
        int i = 0;
        //calculamos la frecuencia de aparicion de los niveles de gris en la imagen
        for (int x = 0; x < ancho; x++) {
            for (int y = 0; y < alto; y++) {
                int valor = imagenEntrada[x][y];
                histograma[valor]++;
            }
        }
        int suma = 0;

        //construimos la LUT(lookup table)
        float[] LUT = new float[256];
        for (i = 0; i < 256; i++) {
            suma += histograma[i];
            LUT[i] = suma * 255 / tampixel;
        }
        //Transformamos la imagen usando la LUT
        for (int x = 0; x < ancho; x++) {
            for (int y = 0; y < alto; y++) {
                int valor = imagenEntrada[x][y];
                int valorNuevo = (int) LUT[valor];
                imagenEcualizada[x][y] = valorNuevo;
            }
        }
        return imagenEcualizada;
    }

    /**
     * metodo para convertir la matriz en escala de grises a blanco y negro con un umbral, por defecto es 50
     *
     * @param imagenEntrada con la matriz de entrada
     * @param umbral        un entero con el umbral para detectar cuando se pasa a blanco o negro, por defecto si el girs es por debajo de 50 se pasa a negro
     * @return imagenSalida con la matriz pasada a blanco y negro
     */
    private int[][] convertirImagenAByN(int[][] imagenEntrada, int umbral) {
        int[][] imagenByN = new int[anchuraOriginal][alturaOriginal];
        for (int x = 0; x < imagenEntrada.length; x++) {
            for (int y = 0; y < imagenEntrada[0].length; y++) {
                int valor = imagenEntrada[x][y];
                if (valor < umbral) {
                    imagenByN[x][y] = 0;//si esta por debajo del umbral, negro
                } else {
                    imagenByN[x][y] = 1;//si no, blanco
                }
            }
        }
        return imagenByN;
    }

    /**
     * filtrado para rellenar pequeños huecos de un pixel en zonas oscuras y cortes en segmentos rectos
     * filtro = p+b.g.(d+e)+d.e.(b+g)
     *
     * @param imagenEntrada con la matriz de entrada
     * @return imagenSalida con el primer filtro binario aplicado
     */
    private int[][] filtroBinario1(int[][] imagenEntrada) {
        int[][] imagenFiltrada = new int[anchuraOriginal][alturaOriginal];
        int b, p, e, d, g;//puntos cuadrados desde punto p(vertical y horizontal)
        int ancho = imagenEntrada.length;
        int alto = imagenEntrada[0].length;
        int filtrado = 0;

        for (int x = 1; x < ancho - 1; x++) {//para no salirnos de indices(evitar indices negativos al restar empezamos en 1 y vamos hasta indice -1), se introducirá un borde negro en la imagen que no afecta
            for (int y = 1; y < alto - 1; y++) {//asignamos los puntos vecinos
                p = imagenEntrada[x][y];//centro
                b = imagenEntrada[x - 1][y];//arriba
                g = imagenEntrada[x + 1][y];//abajo
                d = imagenEntrada[x][y - 1];//izquierda
                e = imagenEntrada[x][y + 1];//derecha
                filtrado = p | b & g & (d | e) | d & e & (b | g);
                imagenFiltrada[x][y] = filtrado;
            }
        }
        return imagenFiltrada;
    }

    /**
     * filtrado para eliminar los unos aislados y protuberancias en segmentos de lados rectos
     * filtro = p+b.g.(d+e)+d.e.(b+g)
     *
     * @param imagenEntrada con la matriz de entrada
     * @return imagenSalida con el segundo filtro binario aplicado
     */
    private int[][] filtroBinario2(int[][] imagenEntrada) {
        int[][] imagenFiltrada = new int[anchuraOriginal][alturaOriginal];
        int p, b, g, d, e, a, f, c, h;// todos los puntos en vecindad 3x3
        int ancho = imagenEntrada.length;
        int alto = imagenEntrada[0].length;
        int filtrado = 0;

        for (int x = 1; x < ancho - 1; x++) {
            for (int y = 1; y < alto - 1; y++) {//asignamos los puntos vecinos en una vecindad de 3x3
                p = imagenEntrada[x][y];//centro
                b = imagenEntrada[x - 1][y];//arriba
                g = imagenEntrada[x + 1][y];//abajo
                d = imagenEntrada[x][y - 1];//izquierda
                e = imagenEntrada[x][y + 1];//derecha
                a = imagenEntrada[x - 1][y - 1];//esquina arriba-izq
                f = imagenEntrada[x + 1][y - 1];//esquina abajo-izq
                c = imagenEntrada[x - 1][y + 1];//esquina arriba-dcha
                h = imagenEntrada[x + 1][y + 1];//esquina abajo-dcha
                filtrado = p & ((a | b | d) & (e | g | h) | (b | c | e) & (d | f | g));
                imagenFiltrada[x][y] = filtrado;
            }
        }
        return imagenFiltrada;
    }

    /**
     * algoritmo de Zhang-Shuen para el adelgazamiento de la imagen, explicado en la documentación externa y pasos en la interna
     * basado en el punto 7 del guión de la práctica, 2 sub-iteraciones, en cada una se evalúa cada pixel en base a 4 condiciones
     * al cumplirse, el pixel puede borrarse al no ser elemento fundamental del esqueleto de la imagen
     * la imagen con pixeles ya borrados sera la entrada de la siguiente sub-iteracion, cuando no se borra ninguno se da la ultima iteración
     *
     * @param imagenEntrada con la matriz de entrada
     * @return imagenSalida con la imagen adelgazada
     */
    private int[][] adelgazamientoZhangSuen(int[][] imagenEntrada) {
        int[][] imagenAdelgazada = new int[anchuraOriginal][alturaOriginal];//salida
        int[][] pixelesACambiar = new int[anchuraOriginal][alturaOriginal];//matriz auxiliar para marcar los pixeles a cambiar si cumplen condicion
        int P1, P2, P3, P4, P5, P6, P7, P8, P9, Ap1 = 0, Bp1 = 0;//puntos de vecinos en ventana vecindad 3x3, Ap1=A(P1), Bp1=B(P1)
        int ancho = imagenEntrada.length;
        int alto = imagenEntrada[0].length;
        boolean cambiado = false;

        //primero invertimos la imagen de entrada, los 0 a 1 y viceversa
        for (int x = 0; x < ancho; x++) {
            for (int y = 0; y < alto; y++) {
                if (imagenEntrada[x][y] == 0) {
                    imagenAdelgazada[x][y] = 1;
                } else {
                    imagenAdelgazada[x][y] = 0;
                }
                pixelesACambiar[x][y] = 0;//inicializamos a 0 los puntos marcados
            }
        }

        do {//while(cambiado)
            //comienza primera sub-iteracion
            cambiado = false;//ponemos a false por si no encuentra ninguno a cambiar, se pondra a true si cumple la condicion
            for (int x = 1; x < ancho - 1; x++) {//recorrido, x=1 y rango  hasta ancho -1 para evitar outofboundsexception
                for (int y = 1; y < alto - 1; y++) {
                    P1 = imagenAdelgazada[x][y];
                    if (P1 == 1) {//si p1(centro) es 1(blanco, pero en realidad negro ya que se ha invertido)
                        P2 = imagenAdelgazada[x - 1][y];//vamos cargando todos los puntos de alredador de p1 en ventana de vecindad 3x3
                        P3 = imagenAdelgazada[x - 1][y + 1];
                        P4 = imagenAdelgazada[x][y + 1];
                        P5 = imagenAdelgazada[x + 1][y + 1];
                        P6 = imagenAdelgazada[x + 1][y];
                        P7 = imagenAdelgazada[x + 1][y - 1];
                        P8 = imagenAdelgazada[x][y - 1];
                        P9 = imagenAdelgazada[x - 1][y - 1];

                        //ahora comprobaremos el numero de cambios "01" entre ellos, p2 con p3, p3 con p4...
                        //si hay cambio, aumentamos Ap1 que es el contador para ello
                        if (P2 == 0 && P3 == 1)
                            Ap1++;
                        if (P3 == 0 && P4 == 1)
                            Ap1++;
                        if (P4 == 0 && P5 == 1)
                            Ap1++;
                        if (P5 == 0 && P6 == 1)
                            Ap1++;
                        if (P6 == 0 && P7 == 1)
                            Ap1++;
                        if (P7 == 0 && P8 == 1)
                            Ap1++;
                        if (P8 == 0 && P9 == 1)
                            Ap1++;
                        if (P9 == 0 && P2 == 1)
                            Ap1++;
                        //hacemos la suma de B(P1) que es el numero de pixeles blancos vecinos a p1, es decir, los que sean 1 se sumaran, los 0 no aumentaran Bp1
                        Bp1 = P2 + P3 + P4 + P5 + P6 + P7 + P8 + P9;
                        if ((Bp1 >= 2 && Bp1 <= 6) && Ap1 == 1 && (P2 * P4 * P6) == 0 && (P4 * P6 * P8) == 0) {//condicion de la primera sub-iteracion
                            pixelesACambiar[x][y] = 1; //si la cumple, lo marcamos para cambiar(borrar)
                            cambiado = true; //hemos cambiado uno, se hara otra iteracion
                        }
                        Ap1 = 0;//reiniciamos contador A(P1)
                    }
                }
            }
            //hacemos el cambio si hay algún punto a cambiar marcado en puntosACambiar
            for (int x = 0; x < ancho; x++) {
                for (int y = 0; y < alto; y++) {
                    if (pixelesACambiar[x][y] == 1) {//si esta marcado
                        imagenAdelgazada[x][y] = 0;//invertimos ese pixel(borramos)
                        pixelesACambiar[x][y] = 0;//lo desmarcamos
                    }
                }
            }
            //segunda sub-iteracion
            for (int x = 1; x < ancho - 1; x++) {//recorrido
                for (int y = 1; y < alto - 1; y++) {
                    P1 = imagenAdelgazada[x][y];//centro
                    if (P1 == 1) {//mismas acciones
                        P2 = imagenAdelgazada[x - 1][y];//vamos cargando todos los puntos de alrededor de p1
                        P3 = imagenAdelgazada[x - 1][y + 1];
                        P4 = imagenAdelgazada[x][y + 1];
                        P5 = imagenAdelgazada[x + 1][y + 1];
                        P6 = imagenAdelgazada[x + 1][y];
                        P7 = imagenAdelgazada[x + 1][y - 1];
                        P8 = imagenAdelgazada[x][y - 1];
                        P9 = imagenAdelgazada[x - 1][y - 1];

                        // comprobaremos el numero de cambios "01" entre ellos, p2 con p3, p3 con p4...
                        if (P2 == 0 && P3 == 1)
                            Ap1++;
                        if (P3 == 0 && P4 == 1)
                            Ap1++;
                        if (P4 == 0 && P5 == 1)
                            Ap1++;
                        if (P5 == 0 && P6 == 1)
                            Ap1++;
                        if (P6 == 0 && P7 == 1)
                            Ap1++;
                        if (P7 == 0 && P8 == 1)
                            Ap1++;
                        if (P8 == 0 && P9 == 1)
                            Ap1++;
                        if (P9 == 0 && P2 == 1)
                            Ap1++;
                        //suma en B(P1)
                        Bp1 = P2 + P3 + P4 + P5 + P6 + P7 + P8 + P9;
                        if ((Bp1 >= 2 && Bp1 <= 6) && Ap1 == 1 && (P2 * P4 * P8) == 0 && (P2 * P6 * P8) == 0) {//condicion de la segunda subiteracion
                            pixelesACambiar[x][y] = 1; //lo marcamos para cambiar(borrar) si se cumple
                            cambiado = true; //hemos cambiado uno
                        }
                        Ap1 = 0;//reiniciamos contador A(P1)

                    }
                }
            }
            //si hay marcado, cambiamos
            for (int x = 0; x < ancho; x++) {
                for (int y = 0; y < alto; y++) {
                    if (pixelesACambiar[x][y] == 1) {//si esta marcado
                        imagenAdelgazada[x][y] = 0;//invertimos ese pixel
                        pixelesACambiar[x][y] = 0;//lo desmarcamos
                    }
                }
            }

        } while (cambiado);//bucle do-while, mientras se marque algun punto en una de las 2 sub-iteraciones sigue iterando
        //restauramos la imagen original previamente invertida, ahora ya adelgazada
        for (int x = 0; x < ancho; x++) {
            for (int y = 0; y < alto; y++) {
                if (imagenAdelgazada[x][y] == 0) {
                    imagenAdelgazada[x][y] = 1;
                } else {
                    imagenAdelgazada[x][y] = 0;
                }
            }
        }
        return imagenAdelgazada;
    }

    /**
     * menu principal de opciones
     *
     * @return op, un entero con la opcion seleccionada
     */
    public int menuSeleccion() {
        int op;
        System.out.println("**********************************************************");
        System.out.println("Seleccione la funcion que desea realizar, para tratar la imagen debe elegir la opcion 1 primero");
        System.out.println("0.Salir");
        System.out.println("1.Seleccionar imagen(debe estar en el mismo directorio que el ejecutable, o indique la ruta completa)");
        System.out.println("2.Pasar imagen RGB a matriz de grises de tipo byte");
        System.out.println("3.Calcular histograma de la imagen en escala de grises(Ecualizacion)");
        System.out.println("4.Convertir imagen de grises a una matriz de blanco y negro de tipo byte(Binarizacion)");
        System.out.println("5.Eliminar ruido binario(Filtrado)");
        System.out.println("6.Adelgazamiento de la imagen con algoritmo Zhang-Shuen");
        System.out.println("7.AUTO: Ecualizacion->Binarizacion->Filtrado->Adelgazamiento(Debe haber seleccionado imagen previamente en opcion 1)");
        op = opcion.nextInt();
        return op;
    }

    /**
     * proceso que maneja las opciones y todas las operaciones a realizar, es el llamado desde el main
     *
     * @return nada
     */
    public void procesoPrincipal() {
        int[][] imagenSalida = new int[anchuraOriginal][alturaOriginal];
        int seleccion;
        boolean enc = false;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String imagenSeleccionada = null;
        boolean esc = false;
        System.out.println("********** Practica Biometria huellas dactilares *********");
        while (!esc) {
            try {
                seleccion = menuSeleccion();
                switch (seleccion) {
                    case 0://salir
                        esc = true;
                        System.out.println("**********************************************************");
                        System.out.println("*************************** FIN **************************");
                        break;
                    case 1://seleccionar imagen
                        System.out.println("Introduzca el nombre de la imagen que quiere usar con la extension(debe estar en este mismo directorio)");
                        System.out.print("Imagen: ");
                        while (!enc) {
                            imagenSeleccionada = br.readLine();
                            if (Files.exists(Paths.get(imagenSeleccionada))) {
                                enc = true;
                            } else {
                                System.out.println("ERROR! fichero no encontrado");
                                System.out.println("Introduzca el nombre de nuevo, asegurese que esta en el mismo directorio");
                                System.out.print("Imagen: ");
                            }
                        }
                        seleccionarArchivo(imagenSeleccionada);
                        break;
                    case 2://pasar a matriz de grises de tipo byte
                        imagenSalida = convertirImagenAGrises(imagenOriginal);
                        pasarImagenAArchivo(imagenSalida, "imagenEscalaGrises.jpg", 1);
                        System.out.println("\nimagenEscalaGrises.jpg generada\n");
                        break;
                    case 3:
                        imagenSalida = calcularHistograma(imagenSalida);
                        pasarImagenAArchivo(imagenSalida, "imagenHistograma.jpg", 1);
                        System.out.println("\nimagenHistograma.jpg generada\n");
                        break;
                    case 4:
                        imagenSalida = convertirImagenAByN(imagenSalida, umbral);
                        pasarImagenAArchivo(imagenSalida, "imagenByN.jpg", 0);
                        System.out.println("\nimagenByN.jpg generada\n");
                        break;
                    case 5:
                        imagenSalida = filtroBinario1(imagenSalida);
                        imagenSalida = filtroBinario2(imagenSalida);
                        pasarImagenAArchivo(imagenSalida, "imagenFiltrada.jpg", 0);
                        System.out.println("\nimagenFiltrada.jpg generada\n");
                        break;
                    case 6:
                        imagenSalida = adelgazamientoZhangSuen(imagenSalida);
                        pasarImagenAArchivo(imagenSalida, "imagenAdelgazadaZS.jpg", 0);
                        System.out.println("\nimagenAdelgazadaZS.jpg generada\n");
                        break;
                    case 7:
                        imagenSalida = convertirImagenAGrises(imagenOriginal);//grises
                        imagenSalida = calcularHistograma(imagenSalida);//histograma
                        imagenSalida = convertirImagenAByN(imagenSalida, umbral);//blanco y negro
                        imagenSalida = filtroBinario1(imagenSalida);//filtrado 1
                        imagenSalida = filtroBinario2(imagenSalida);//filtrado 2
                        imagenSalida = adelgazamientoZhangSuen(imagenSalida);//adelgazamiento con Zhang-Shuen
                        pasarImagenAArchivo(imagenSalida, "imagenAUTO.jpg", 0);
                        System.out.println("\nimagenAUTO.jpg generada\n");
                        break;
                    default:
                        System.out.println("Opcion no valida, vuelva a seleccionar una opcion (0-7)");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * main
     */
    public static void main(String[] args) {
        FingerPrintImage fpi = new FingerPrintImage();
        fpi.procesoPrincipal();
    }
}