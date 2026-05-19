import re
import os
import glob

def find_balanced_block(text, start_idx, open_char='(', close_char=')'):
    depth = 0
    in_string = False
    string_char = None
    i = start_idx
    while i < len(text):
        c = text[i]
        if in_string:
            if c == string_char and text[i-1] != '\\':
                in_string = False
            elif c == '\\' and i + 1 < len(text):
                i += 1
        else:
            if c in '"\'"':
                in_string = True
                string_char = c
            elif c == open_char:
                depth += 1
            elif c == close_char:
                depth -= 1
                if depth == 0:
                    return i
        i += 1
    return -1

def replace_component(text, component_name, new_name, remove_params=None):
    if remove_params is None:
        remove_params = []
    
    result = []
    i = 0
    while i < len(text):
        idx = text.find(component_name + '(', i)
        if idx == -1:
            result.append(text[i:])
            break
        
        result.append(text[i:idx])
        start = idx + len(component_name + '(')
        end = find_balanced_block(text, start - 1)
        if end == -1:
            result.append(text[idx:])
            break
        
        block_content = text[start:end]
        
        params = []
        param_start = 0
        param_depth = 0
        param_j = 0
        in_str = False
        str_char = None
        
        while param_j < len(block_content):
            c = block_content[param_j]
            if in_str:
                if c == str_char and block_content[param_j-1] != '\\':
                    in_str = False
                elif c == '\\':
                    param_j += 1
            else:
                if c in '"\'"':
                    in_str = True
                    str_char = c
                elif c in '({[':
                    param_depth += 1
                elif c in ')}]':
                    param_depth -= 1
                elif c == ',' and param_depth == 0:
                    params.append(block_content[param_start:param_j].strip())
                    param_start = param_j + 1
            param_j += 1
        
        if param_start < len(block_content):
            params.append(block_content[param_start:].strip())
        
        if remove_params:
            filtered = []
            for p in params:
                param_name = p.split('=')[0].strip()
                if param_name not in remove_params:
                    filtered.append(p)
            params = filtered
        
        # Determine indent from context
        line_start = text.rfind('\n', 0, idx) + 1
        indent_len = idx - line_start
        indent = ' ' * indent_len
        inner_indent = indent + '    '
        
        if len(params) == 0:
            new_block = new_name + '()'
        elif len(params) == 1:
            new_block = new_name + '(\n' + inner_indent + params[0] + '\n' + indent + ')'
        else:
            new_block = new_name + '(\n' + ',\n'.join(inner_indent + p for p in params) + '\n' + indent + ')'
        result.append(new_block)
        i = end + 1
    
    return ''.join(result)

def add_imports(content, imports_needed):
    lines = content.split('\n')
    import_idx = 0
    for idx, line in enumerate(lines):
        if line.startswith('import '):
            import_idx = idx + 1
    
    existing = set(line.strip() for line in lines if line.strip().startswith('import '))
    to_add = [imp for imp in imports_needed if imp not in existing]
    if to_add:
        lines.insert(import_idx, '\n'.join(to_add))
    return '\n'.join(lines)

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    
    orig_switch = content.count('Switch(')
    orig_slider = content.count('Slider(')
    orig_textbutton = content.count('TextButton(')
    
    # Replace Switch with RetroToggle, removing thumbContent
    content = replace_component(content, 'Switch', 'RetroToggle', remove_params=['thumbContent'])
    
    # Replace Slider with RetroSlider, removing steps, thumb, track, colors
    content = replace_component(content, 'Slider', 'RetroSlider', remove_params=['steps', 'thumb', 'track', 'colors'])
    
    # Replace TextButton with RetroTextButton (but NOT inside comments or strings)
    # Only replace standalone TextButton(
    content = replace_component(content, 'TextButton', 'RetroTextButton')
    
    # Replace RoundedCornerShape(16.dp) with RoundedCornerShape(0.dp)
    content = content.replace('.clip(RoundedCornerShape(16.dp))', '.clip(RoundedCornerShape(0.dp))')
    content = content.replace('RoundedCornerShape(16.dp),', 'RoundedCornerShape(0.dp),')
    content = content.replace('shape = RoundedCornerShape(16.dp)', 'shape = RoundedCornerShape(0.dp)')
    
    # Fix any mangled component names
    content = content.replace('WavyRetroSlider', 'WavySlider')
    content = content.replace('SquigglyRetroSlider', 'SquigglySlider')
    
    # Add imports if needed
    imports_needed = []
    if content.count('RetroToggle(') > 0:
        imports_needed.append('import com.metrolist.music.ui.theme.RetroToggle')
    if content.count('RetroSlider(') > 0:
        imports_needed.append('import com.metrolist.music.ui.theme.RetroSlider')
    if content.count('RetroTextButton(') > 0:
        imports_needed.append('import com.metrolist.music.ui.theme.RetroTextButton')
    if content.count('RetroButton(') > 0:
        imports_needed.append('import com.metrolist.music.ui.theme.RetroButton')
    
    if imports_needed:
        content = add_imports(content, imports_needed)
    
    with open(filepath, 'w') as f:
        f.write(content)
    
    print(f'Done: {os.path.basename(filepath)}')
    print(f'  Switches: {orig_switch} -> {content.count("Switch(")}')
    print(f'  Sliders: {orig_slider} -> {content.count("Slider(")}')
    print(f'  TextButtons: {orig_textbutton} -> {content.count("TextButton(")}')

# Process all settings files
settings_files = glob.glob('app/src/main/kotlin/com/metrolist/music/ui/screens/settings/*.kt')
settings_files += glob.glob('app/src/main/kotlin/com/metrolist/music/ui/screens/settings/integrations/*.kt')

for filepath in sorted(settings_files):
    with open(filepath, 'r') as f:
        content = f.read()
    if 'Switch(' in content or 'Slider(' in content or 'TextButton(' in content or 'ElevatedCard(' in content:
        process_file(filepath)
