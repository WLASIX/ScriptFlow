package com.wlaxid.scriptflow

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.widget.addTextChangedListener
import androidx.drawerlayout.widget.DrawerLayout
import com.amrdeveloper.codeview.CodeView
import com.wlaxid.scriptflow.editor.EditorController
import com.wlaxid.scriptflow.editor.EditorState
import com.wlaxid.scriptflow.editor.FileController
import com.wlaxid.scriptflow.runtime.RunState
import com.wlaxid.scriptflow.ui.actions.EditorActionsController
import com.wlaxid.scriptflow.ui.console.ConsoleController
import com.wlaxid.scriptflow.ui.console.ConsoleOutputPresenter
import com.wlaxid.scriptflow.runtime.RunSessionController

class MainActivity : AppCompatActivity() {

    private lateinit var editorState: EditorState
    private lateinit var editorController: EditorController
    private lateinit var fileController: FileController
    private lateinit var codeView: CodeView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var itemOpen: LinearLayout
    private lateinit var itemSave: LinearLayout
    private lateinit var btnQuickSave: View
    private lateinit var fabActionsRoot: View
    private lateinit var editorActionsController: EditorActionsController
    private lateinit var consoleController: ConsoleController
    private lateinit var consoleRoot: View
    private lateinit var consolePresenter: ConsoleOutputPresenter
    private lateinit var runSessionController: RunSessionController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editorState = EditorState(getString(R.string.default_filename))
        bindViews()
        setupConsole()
        setupRunSession()
        observeKeyboard()
        setupEditor()
        setupListeners()
        setupFileController()
        setupEditorActions()
        editorActionsController.setTitle(editorState.displayName())
    }

    private fun bindViews() {
        codeView = findViewById(R.id.codeView)
        drawerLayout = findViewById(R.id.drawerLayout)
        itemOpen = findViewById(R.id.itemOpen)
        itemSave = findViewById(R.id.itemSave)
        fabActionsRoot = findViewById(R.id.fabActionsInclude)
        consoleRoot = findViewById(R.id.consoleSheet)

        btnQuickSave = findViewById(R.id.btnQuickSave)
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
            editorState.onInitialTextSet()
            editorActionsController.setTitle(editorState.displayName())
        }
    }

    private fun setupFileController() {
        fileController = FileController(
            activity = this,
            editorController = editorController,
            onFileOpened = { uri, name ->
                editorState.onFileOpened(uri, name)
                editorActionsController.setTitle(editorState.displayName())
            },
            onFileSaved = { uri, name ->
                editorState.onFileSaved(uri, name)
                editorActionsController.setTitle(editorState.displayName())
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
                editorActionsController.setTitle(editorState.displayName())
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

        btnQuickSave.setOnClickListener {
            fileController.quickSave(
                editorState.currentFileUri,
                editorState.currentFileName
            )
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

    private fun setupRunSession() {
        runSessionController = RunSessionController(
            context = this,
            consolePresenter = consolePresenter,
            onUiStateChanged = { state ->
                editorActionsController.renderRunState(state)
            }
        )
    }
    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(codeView.windowToken, 0)
    }

    private fun setupEditorActions() {
        val toolbarRoot = findViewById<View>(R.id.topToolbar)
        val fabRoot = findViewById<View>(R.id.fabActionsInclude)

        editorActionsController = EditorActionsController(
            toolbarRoot = toolbarRoot,
            fabRoot = fabRoot,
            drawerLayout = drawerLayout,
            onUndo = { },
            onRedo = { },
            onSearch = { },
            onScreenshot = { },
            onEyedropper = { },
            onExecute = {
                when (runSessionController.currentState()) {
                    RunState.Running -> runSessionController.stop()
                    else -> runSessionController.execute(editorController.getText())
                }
            }
        )
    }

    override fun onDestroy() {
        try {
            runSessionController.destroy()
        } catch (_: Exception) {
            // ничего - если runController ещё не инициализирован или уже разрушен
        }
        super.onDestroy()
    }
}