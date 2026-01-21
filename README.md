# Twig Navigator Plugin

PhpStorm plugin for enhanced navigation between Twig templates and PHP code.

## Features

### Template Path Navigation
- Navigate from PHP code to Twig templates
- Navigate between Twig templates (includes, extends, etc.)
- Supports custom template paths and frameworks (Symfony, CodeIgniter, etc.)

### Twig to PHP Navigation (v1.0.4+)

#### Navigate to PHP Classes
Cmd+Click on class names in `{# @var ... #}` comments to jump to PHP class definitions.

```twig
{# @var info \App\Forms\Info #}
```

Supports both annotation formats:
- `{# @var variableName ClassName #}`
- `{# @var ClassName variableName #}`

Handles file paths:
- `{# @var info \path\to\Action.php #}`
- `{# @var info application\Containers\MyClass #}`

#### Navigate to PHP Methods
Cmd+Click on method calls to jump to the method definition in the PHP class.

```twig
{# @var info \MyNamespace\MyClass #}
{{ info.run() }}  {# Cmd+Click on 'run' to navigate to MyClass::run() #}
```

#### Navigate to Variable Definitions
Cmd+Click on variables to jump to their `@var` annotation.

```twig
{# @var user \App\Models\User #}
{{ user.getName() }}  {# Cmd+Click on 'user' to jump to the @var comment #}
```

## Installation

1. Build the plugin:
   ```bash
   ./gradlew buildPlugin
   ```

2. Install from disk in PhpStorm:
   - Go to `Settings` → `Plugins` → `⚙️` → `Install Plugin from Disk...`
   - Select `build/distributions/twig5-1.0.4.zip`

## Configuration

Go to `Settings` → `Tools` → `Twig Navigator` to configure:
- Template directories
- Custom template path patterns
- Framework-specific settings

## Requirements

- PhpStorm 2024.1 or newer
- PHP plugin enabled
- Twig plugin enabled

## Version History

### 1.0.3
- ✨ Navigate to PHP classes from @var comments
- ✨ Navigate to PHP methods from Twig method calls
- ✨ Navigate to variable definitions
- ✨ Flexible @var parsing (supports both name/type orders)
- ✨ File path support in @var annotations

### 1.0.2
- Updated icons

## License

MIT