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

def fix_retrotextbuttons(content):
    result = []
    i = 0
    while i < len(content):
        idx = content.find('RetroTextButton(', i)
        if idx == -1:
            result.append(content[i:])
            break
        
        result.append(content[i:idx])
        start = idx + len('RetroTextButton(')
        end = find_balanced_block(content, start - 1)
        if end == -1:
            result.append(content[idx:])
            break
        
        block = content[start:end]
        
        # Check if there's a lambda block after the parentheses
        lambda_start = end + 1
        has_lambda = False
        lambda_content = ""
        if lambda_start < len(content) and content[lambda_start] == ' ':
            lambda_start += 1
        if lambda_start < len(content) and content[lambda_start] == '{':
            lambda_end = find_balanced_block(content, lambda_start, '{', '}')
            if lambda_end != -1:
                has_lambda = True
                lambda_content = content[lambda_start+1:lambda_end].strip()
        
        if not has_lambda:
            # Simple case - no lambda, keep as is
            result.append(content[idx:end+1])
            i = end + 1
            continue
        
        # Parse params from block
        params = []
        param_start = 0
        param_depth = 0
        param_j = 0
        in_str = False
        str_char = None
        
        while param_j < len(block):
            c = block[param_j]
            if in_str:
                if c == str_char and block[param_j-1] != '\\':
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
                    params.append(block[param_start:param_j].strip())
                    param_start = param_j + 1
            param_j += 1
        
        if param_start < len(block):
            params.append(block[param_start:].strip())
        
        # Try to extract text from lambda
        # Pattern: Text("...") or Text(stringResource(...)) or Text(text = ...)
        text_match = re.search(r'Text\(\s*(?:text\s*=\s*)?([^)]+)\s*\)', lambda_content)
        if text_match:
            text_expr = text_match.group(1).strip()
            # Build new RetroTextButton with text= param
            new_params = [f'text = {text_expr}'] + params
            line_start = content.rfind('\n', 0, idx) + 1
            indent_len = idx - line_start
            indent = ' ' * indent_len
            inner_indent = indent + '    '
            new_block = 'RetroTextButton(\n' + ',\n'.join(inner_indent + p for p in new_params) + '\n' + indent + ')'
            result.append(new_block)
            i = lambda_end + 1
        else:
            # Complex lambda - convert to RetroButton
            new_params = params
            line_start = content.rfind('\n', 0, idx) + 1
            indent_len = idx - line_start
            indent = ' ' * indent_len
            inner_indent = indent + '    '
            new_block = 'RetroButton(\n' + ',\n'.join(inner_indent + p for p in new_params) + '\n' + indent + ') {\n' + inner_indent + lambda_content + '\n' + indent + '}'
            result.append(new_block)
            i = lambda_end + 1
    
    return ''.join(result)

# Process all settings files
settings_files = glob.glob('app/src/main/kotlin/com/metrolist/music/ui/screens/settings/*.kt')
settings_files += glob.glob('app/src/main/kotlin/com/metrolist/music/ui/screens/settings/integrations/*.kt')

for filepath in sorted(settings_files):
    with open(filepath, 'r') as f:
        content = f.read()
    
    if 'RetroTextButton(' in content:
        new_content = fix_retrotextbuttons(content)
        with open(filepath, 'w') as f:
            f.write(new_content)
        print(f'Fixed: {os.path.basename(filepath)}')
