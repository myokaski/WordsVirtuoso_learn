package wordsvirtuoso

class Virtuoso(private val args: Array<String>) {
    enum class State { STOPPED, MAIN }

    private var state: State = State.STOPPED
    private var words = setOf<String>()
    private var candidates = setOf<String>()
    private var secret = ""
    private var startTime = 0L
    private val clues = mutableListOf<String>()
    private val wrongChars = mutableSetOf<Char>()
    private var countTries = 0
    var isRunning = false
        private set
        get() = state != State.STOPPED

    fun start(): String {
        checkArgs()?.let { return it }
        checkFileAvailability(args[0], "words")?.let { return it }
        checkFileAvailability(args[1], "candidate words")?.let { return it }
        args.forEach { checkWordsInFile(it)?.let { msg -> return msg } }
        checkIncludes(args[0], args[1])?.let { return it }
        secret = candidates.randomOrNull() ?: return "Empty candidates list"
        // println("secret = $secret")
        clues.clear()
        state = State.MAIN
        countTries = 0
        startTime = System.currentTimeMillis()
        return "Words Virtuoso"
    }

    fun getPrompt(): String {
        return "\n" + when (state) {
            State.MAIN -> "Input a 5-letter word:"
            State.STOPPED -> ""
        }
    }

    private fun winMessage(): String {
        val elapsedTime = (System.currentTimeMillis() - startTime) / 1000
        return "Correct!\n" + if (countTries == 1) {
            "Amazing luck! The solution was found at once."
        } else "The solution was found after $countTries tries in $elapsedTime seconds."
    }

    fun processInput(line: String): String {
        val input = line.lowercase()
        if (input == "exit") return "The game is over.".also { state = State.STOPPED }
        when (state) {
            State.MAIN -> {
                countTries++
                checkWord(input, checkIsInWords = true)?.let { return it }
                return getClue(input) + if (input == secret) {
                    winMessage().also { state = State.STOPPED }
                } else getWrongChars()
            }

            State.STOPPED -> check(false)
        }
        return ""
    }

    enum class Colors(val sequence: String) {
        GREEN("\u001B[48:5:10m"),
        YELLOW("\u001B[48:5:11m"),
        GREY("\u001B[48:5:7m"),
        AZURE("\u001B[48:5:14m"),
        COMMON("\u001B[0m")
    }

    private fun getColoredString(string: String, color: Colors): String {
        return color.sequence + string + Colors.COMMON.sequence
    }

    private fun getClue(input: String): String {
        // Input is suggested to be valid word here
        var clue = ""
        for ((index, char) in input.withIndex()) {
            clue += when {
                secret[index] == char -> getColoredString(char.uppercase(), Colors.GREEN)
                secret.contains(char) -> getColoredString(char.uppercase(), Colors.YELLOW)
                else -> getColoredString(char.uppercase(), Colors.GREY).also {
                    wrongChars.add(char.uppercaseChar())
                }
            }
        }
        clues.add(clue)
        return "\n${clues.joinToString("\n")}\n\n"
    }

    private fun getWrongChars(): String {
        return getColoredString(wrongChars.sorted().joinToString(""), Colors.AZURE)
    }

    private fun checkWordsInFile(filename: String): String? {
        val file = java.io.File(filename)
        var countInvalid = 0
        file.forEachLine {
            if (checkWord(it.lowercase()) != null) countInvalid++
        }
        return if (countInvalid == 0) null else {
            "Error: $countInvalid invalid words were found in the $filename file."
        }
    }

    private fun checkFileAvailability(filename: String, description: String): String? {
        val file = java.io.File(filename)
        if (!file.canRead()) return "Error: The $description file $filename doesn't exist."
        return null
    }

    private fun checkArgs(): String? {
        if (args.size != 2) return "Error: Wrong number of arguments."
        return null
    }

    private fun checkIncludes(wordsFilename: String, candidatesFilename: String): String? {
        words = java.io.File(wordsFilename).readLines().map { it.lowercase() }.toSet()
        candidates = java.io.File(candidatesFilename).readLines().map { it.lowercase() }.toSet()
        val absent = candidates.minus(words)
        if (absent.isEmpty()) return null
        return "Error: ${absent.size} candidate words are not included in the $wordsFilename file."
    }

    private fun checkWord(input: String, checkIsInWords: Boolean = false): String? {
        val validLen = 5
        return when {
            input.length != validLen -> "The input isn't a $validLen-letter word."
            input.contains(Regex("[^a-z]")) -> "One or more letters of the input aren't valid."
            input.toSet().size != validLen -> "The input has duplicate letters."
            checkIsInWords && input !in words -> "The input word isn't included in my words list."
            else -> null
        }
    }
}

fun main(args: Array<String>) {
    val game = Virtuoso(args)
    println(game.start())
    while (game.isRunning) {
        println(game.getPrompt())
        println(game.processInput(readln()))
    }
}
