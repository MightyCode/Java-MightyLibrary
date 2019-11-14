package MightyLibrary.mightylib.util.valueDebug;

import MightyLibrary.project.main.Main;

public class TableDebug {

    /** Print part of table part with start and size **/

    public static void printPartd(double[] table, int start, int size){
        if (!Main.admin) return;
        for(int i = start; i < size; i++){
            System.out.print(table[i]);
            System.out.print(", ");
        }
    }

    public static void printPartf(float[] table, int start, int size){
        if (!Main.admin) return;
        for(int i = start; i < size; i++){
            System.out.print(table[i]);
            System.out.print(", ");
        }
    }

    public static void printParti(int[] table, int start, int size){
        if (!Main.admin) return;
        for(int i = start; i < size; i++){
            System.out.print(table[i]);
            System.out.print(", ");
        }
    }

    public static void printPartb(boolean[] table, int start, int size){
        if (!Main.admin) return;
        for(int i = start; i < size; i++){
            System.out.print(table[i]);
            System.out.print(", ");
        }
    }

    /** Print part of table with size **/
    public static void printPartd(double[] table, int size){
        printPartd(table, 0, size);
    }

    public static void printPartf(float[] table, int size){
        printPartf(table, 0, size);
    }

    public static void printParti(int[] table, int size){
        printParti(table, 0, size);
    }

    public static void printPartb(boolean[] table, int size){
        printPartb(table, 0, size);
    }

    /** Print the table **/
    public static void printd(double[] table){
        printPartd(table, 0, table.length);
    }

    public static void printf(float[] table){
        printPartf(table, 0, table.length);
    }

    public static void printi(int[] table){
        printParti(table, 0, table.length);
    }

    public static void printb(boolean[] table){
        printPartb(table, 0, table.length);
    }
}
