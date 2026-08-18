import os

def replace_in_file(path, replacements):
    with open(path, 'r', encoding='utf-8', errors='ignore') as f:
        content = f.read()
    
    for old, new in replacements:
        content = content.replace(old, new)
        
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

notes_path = r'app/src/main/java/com/inkaction/app/ui/screens/NotesScreen.kt'
todos_path = r'app/src/main/java/com/inkaction/app/ui/screens/TodosScreen.kt'

replace_in_file(notes_path, [
    ('Syntetizov\ufffd\ufffdno', 'Syntetizováno'),
    ('Syntetizov\ufffd''\ufffdno', 'Syntetizováno'),
    ('Nov\ufffd pozn\ufffdmka', 'Nová poznámka'),
    ('Nov\ufffd'' pozn\ufffd''mka', 'Nová poznámka'),
    ('Žádné úkoly', 'Žádné úkoly') # Just to make sure we do it right in python
])

replace_in_file(todos_path, [
    ('\ufffddn\ufffd \ufffdkoly', 'Žádné úkoly'),
    ('Aktivn\ufffd \ufffdkoly', 'Aktivní úkoly'),
    ('Dokon\ufffden\ufffd', 'Dokončené')
])
