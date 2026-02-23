package com.wlaxid.scriptflow

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.drawerlayout.widget.DrawerLayout
import com.amrdeveloper.codeview.CodeView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.wlaxid.scriptflow.editor.EditorController
import com.wlaxid.scriptflow.editor.EditorState
import com.wlaxid.scriptflow.editor.FileController
import com.wlaxid.scriptflow.runtime.RunController
import com.wlaxid.scriptflow.runtime.RunState
import com.wlaxid.scriptflow.ui.toolbar.ToolbarController
import com.wlaxid.scriptflow.ui.console.ConsoleController
import com.wlaxid.scriptflow.ui.console.ConsoleOutputPresenter

class MainActivity : AppCompatActivity() {

    private val editorState = EditorState()
    private lateinit var editorController: EditorController
    private lateinit var fileController: FileController
    private lateinit var codeView: CodeView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var itemOpen: LinearLayout
    private lateinit var itemSave: LinearLayout
    private lateinit var fabExecute: FloatingActionButton
    private lateinit var toolbarController: ToolbarController
    private lateinit var runController: RunController
    private lateinit var consoleController: ConsoleController
    private lateinit var consoleRoot: View
    private lateinit var consolePresenter: ConsoleOutputPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        applyUniversalInsets()

        bindViews()
        setupConsole()

        runController = RunController(
            this,
            onStateChanged = { state ->
                renderRunState(state)
                consolePresenter.onStateChanged(state)
            },
            onOutput = { text ->
                consolePresenter.onOutput(text)
            },
            onError = { err ->
                consolePresenter.onError(err)
            }
        )

        observeKeyboard()
        setupEditor()
        setupListeners()
        setupFileController()
        setupToolbar()
        toolbarController.setTitle(editorState.displayName())
    }

    private fun bindViews() {
        codeView = findViewById(R.id.codeView)
        drawerLayout = findViewById(R.id.drawerLayout)
        itemOpen = findViewById(R.id.itemOpen)
        itemSave = findViewById(R.id.itemSave)
        fabExecute = findViewById(R.id.fabExecute)
        consoleRoot = findViewById(R.id.consoleSheet)
    }

    private fun setupConsole() {
        consoleController = ConsoleController(consoleRoot)
        consolePresenter = ConsoleOutputPresenter(consoleController)
    }

    private fun setupEditor() {
        editorController = EditorController(codeView)
        editorController.init()
        val sample =
            """# ---------- SCRIPT 1: базовые функции и условия ----------

print("\n--- SCRIPT 1 ---")

def is_even(n):
    if n % 2 == 0:
        return True
    else:
        return False

for i in range(1, 6):
    if is_even(i):
        print(i, "is even")
    else:
        print(i, "is odd")


# ---------- SCRIPT 2: классы, состояние, nonlocal / global ----------

print("\n--- SCRIPT 2 ---")

counter = 0

class Accumulator:
    def __init__(self, start):
        self.value = start

    def add(self, x):
        self.value += x
        return self.value

acc = Accumulator(10)
print("initial acc.value =", acc.value)

def process():
    nonlocal_flag = 0

    def inner(step):
        nonlocal nonlocal_flag
        global counter

        nonlocal_flag += step
        counter += step
        acc.add(step)

        print("step =", step)
        print("  nonlocal_flag =", nonlocal_flag)
        print("  counter =", counter)
        print("  acc.value =", acc.value)

    for i in range(3):
        inner(i + 1)

process()

print("final counter =", counter)
print("final acc.value =", acc.value)


# ---------- SCRIPT 3: генераторы, try/except/finally, while ----------

print("\n--- SCRIPT 3 ---")

def number_stream(limit):
    i = 0
    while True:
        if i >= limit:
            break
        yield i
        i += 1

def consume():
    total = 0
    try:
        for n in number_stream(5):
            if n == 3:
                print("skip", n)
                continue
            print("got", n)
            total += n
    except Exception as e:
        print("error:", e)
    finally:
        print("total =", total)

consume()

flag = False
while False:
    pass


# ---------- SCRIPT 4: всё сразу для подсветки ----------

print("\n--- SCRIPT 4 ---")

value = 5

assert value is not None

if value > 0 and value < 10:
    result = True
else:
    result = False

print("result =", result)

def outer():
    x = 1
    def inner():
        nonlocal x
        x += 1
        return x
    return inner()

print("outer() =", outer())

for i in range(3):
    if i == 1:
        continue
    if i == 2:
        break
    print("loop i =", i)
"""
        codeView.post {
            editorController.setText(sample)
        }
    }

    private fun applyUniversalInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets ->

            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val gestures = insets.getInsets(WindowInsetsCompat.Type.systemGestures())
            val tappable = insets.getInsets(WindowInsetsCompat.Type.tappableElement())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

            val top = status.top

            val bottom = maxOf(
                nav.bottom,
                gestures.bottom,
                tappable.bottom,
                ime.bottom
            )

            val left = maxOf(status.left, gestures.left)
            val right = maxOf(status.right, gestures.right)

            view.setPadding(left, top, right, bottom)

            insets
        }
    }

    private fun setupFileController() {
        fileController = FileController(
            activity = this,
            editorController = editorController,
            onFileOpened = { uri, name ->
                editorState.onFileOpened(uri, name)
                toolbarController.setTitle(editorState.displayName())
            },
            onFileSaved = { uri, name ->
                editorState.onFileSaved(uri, name)
                toolbarController.setTitle(editorState.displayName())
            }
        )
    }

    private fun observeKeyboard() {
        val root = findViewById<View>(android.R.id.content)

        root.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = android.graphics.Rect()
            root.getWindowVisibleDisplayFrame(rect)

            val screenHeight = root.height
            val keypadHeight = screenHeight - rect.bottom

            val keyboardOpen = keypadHeight > screenHeight * 0.15

            if (keyboardOpen) {
                consoleController.hide()
            }
        }
    }

    private fun setupListeners() {

        codeView.addTextChangedListener {
            if (!editorState.isDirty) {
                editorState.onTextChanged()
                toolbarController.setTitle(editorState.displayName())
            }
        }

        fabExecute.setOnClickListener {
            when (runController.currentState()) {
                RunState.Running -> runController.stop()
                RunState.Finished,
                RunState.Error,
                RunState.Cancelled -> {
                    runController.execute(editorController.getText())
                }
            }
        }

        itemOpen.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            fileController.open()
        }

        itemSave.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            fileController.save(editorState.currentFileName)
        }

        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {

            override fun onDrawerOpened(drawerView: View) {
                codeView.isEnabled = false
                codeView.clearFocus()
                hideKeyboard()
            }

            override fun onDrawerClosed(drawerView: View) {
                codeView.isEnabled = true
                codeView.requestFocus()
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(codeView.windowToken, 0)
    }

    private fun setupToolbar() {
        val toolbarRoot = findViewById<View>(R.id.topToolbar)

        toolbarController = ToolbarController(
            root = toolbarRoot,
            drawerLayout = drawerLayout
        )
    }

    override fun onDestroy() {
        try {
            runController.destroy()
        } catch (_: Exception) {
            // ничего - если runController ещё не инициализирован или уже разрушен
        }
        super.onDestroy()
    }

    private fun renderRunState(state: RunState) {
        fabExecute.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES

        when (state) {
            RunState.Running -> {
                fabExecute.setImageResource(R.drawable.ic_stop)
                fabExecute.backgroundTintList =
                    ColorStateList.valueOf("#E53935".toColorInt())
            }

            RunState.Finished -> {
                fabExecute.setImageResource(R.drawable.ic_start)
                fabExecute.backgroundTintList =
                    ColorStateList.valueOf("#2ECC71".toColorInt())
            }

            RunState.Error -> {
                fabExecute.setImageResource(R.drawable.ic_start)
                fabExecute.backgroundTintList =
                    ColorStateList.valueOf("#F39C12".toColorInt()) // оранжевый
            }

            RunState.Cancelled -> {
                fabExecute.setImageResource(R.drawable.ic_start)
                fabExecute.backgroundTintList =
                    ColorStateList.valueOf("#95A5A6".toColorInt()) // серый
            }
        }
    }
}