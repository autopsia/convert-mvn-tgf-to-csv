import java.io.File
import java.lang.Exception
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern

fun main(args: Array<String>) {
    val dir = File( object {}.javaClass.getResource("/").file )
    val rootList = mutableListOf<Dependency>()
    val mainDependencyList = mutableMapOf<String, MutableSet<String>>()

    dir.walk().forEach { f ->
        if(f.isFile) {
            if (f.extension == "tgf") {
                println(f.name)
                tgfToCsv(f)?.let { rootList.add(it) }
            }
        }
    }

    for (project in rootList) {
        for (dependency in project.dependencies) {
            mainDependencyList.getOrPut(dependency.artifactId, ::mutableSetOf).add(dependency.version)
        }
    }

    val rowList = mutableListOf<String>()
    for (dependency in mainDependencyList) {
        rowList.add("${dependency.key};${dependency.value.joinToString(",")}")
    }

    createCsvFile("main-dependencies", "Dependencia,Version", rowList)


}

fun createCsvFile(fileName: String, headers: String?, content: Collection<String>){
    val csv = File("$fileName.csv")
    csv.createNewFile()

    csv.bufferedWriter().use {
            out ->

        if (headers != null) {
            println(headers)
            out.write(headers)
            out.newLine()
        }

        for (row in content){
            println(row)
            out.write(row)
            out.newLine()
        }

    }
}

// Devuelve el objeto root que representa al proyecto el cual contiene todas las dependencias
fun tgfToCsv(filePath: File): Dependency?{
    val file = object {}.javaClass.getResourceAsStream(filePath.name)?.bufferedReader()?.readLines()
        ?: throw Exception("File not found")

    val dependencyList = mutableMapOf<Int, Dependency>()

    var root: Dependency? = null

    var parseRelations = false
    var isRootElement = true
    for (line in file) {
        if (line.startsWith("#")) {
            parseRelations = true
            continue
        }

        if (parseRelations) {
            val parsed = line.split(Pattern.compile(" "))
            val parentId = Integer.parseInt(parsed[0])
            val childId = Integer.parseInt(parsed[1])
            val parent = dependencyList[parentId]
            val child = dependencyList[childId]

            if (child != null) {
                parent?.dependencies?.add(child)
            }
        } else {
            //add data
            val parsed = line.split(Pattern.compile(" "))
            if (parsed.size <= 1) continue

            val data = parsed[1].split(":")
            val dep = Dependency(data[0], data[1], data[3], data.getOrElse(4) {""}, data[2])
            dependencyList[Integer.parseInt(parsed[0])] = dep

            if (isRootElement) {
                root = dep
                isRootElement = false
            }
        }
    }

    if (root != null) {
        val rowsRaw = mutableListOf<String>()

        //almacena la fila mas larga
        val longest = AtomicInteger(0)

        // la dependencia root representa a t0do el proyecto
        for (rootDep in root.dependencies) {
            //println(root.dependencies.size)
            printRowRaw(rootDep, listOf(), rowsRaw, longest)

        }

        var header = ""
        for (i in 0 until longest.get()/2) {
            header += if (i == 0) "Dependencia,Version"
            else ",SubDependencia,Version"
        }
        createCsvFile(filePath.nameWithoutExtension, header, rowsRaw)

    }
    return root
}

fun printRowRaw(curr: Dependency, prevRowRaw: List<String>, rowsRaw: MutableList<String>, longestRow: AtomicInteger) {
    val currRowRaw = prevRowRaw + curr.artifactId + curr.version
    if (curr.dependencies.isEmpty()) {
        val rowlength = currRowRaw.size
        if (rowlength > longestRow.get()) longestRow.set(rowlength)
        rowsRaw.add(currRowRaw.joinToString(","))
        return
    }
    for (dep in curr.dependencies){
        printRowRaw(dep, currRowRaw, rowsRaw, longestRow)
    }
}