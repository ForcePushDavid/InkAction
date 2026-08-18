package com.inkaction.app.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.inkaction.app.R
import com.inkaction.app.data.NoteStorageManager
import com.inkaction.app.data.SavedTodo

class TodoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TodoRemoteViewsFactory(this.applicationContext)
    }
}

class TodoRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var todos: List<SavedTodo> = emptyList()

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        loadData()
    }

    private fun loadData() {
        val manager = NoteStorageManager(context)
        todos = manager.todosFlow.value.filter { !it.isCompleted }.sortedByDescending { it.timestamp }
    }

    override fun onDestroy() {
        todos = emptyList()
    }

    override fun getCount(): Int = todos.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= todos.size) return RemoteViews(context.packageName, R.layout.widget_todo_item)
        val todo = todos[position]

        val views = RemoteViews(context.packageName, R.layout.widget_todo_item)
        views.setTextViewText(R.id.item_text, todo.text)
        
        val priorityColor = when (todo.priority.lowercase()) {
            "high" -> Color.parseColor("#FF5252") // Red
            "medium" -> Color.parseColor("#FFD740") // Yellow
            "low" -> Color.parseColor("#448AFF") // Blue
            else -> Color.parseColor("#9E9E9E")
        }
        views.setTextColor(R.id.item_checkbox, priorityColor)

        // FillInIntent for clicks
        val fillInIntent = Intent()
        views.setOnClickFillInIntent(R.id.item_text, fillInIntent)
        views.setOnClickFillInIntent(R.id.item_checkbox, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
