# DentalCrmFrontend

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 22.1.8.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Lint

```bash
ng lint          # ESLint + angular-eslint
```

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test --no-watch
```

## Running end-to-end tests

Los e2e corren con [Playwright](https://playwright.dev/). Requieren el servidor de
desarrollo en marcha (`ng serve`, el `webServer` de `playwright.config.ts` lo reutiliza
si ya está corriendo) y un navegador instalado (`npx playwright install chromium`).

```bash
npm run e2e                     # toda la suite
npx playwright test qa          # un solo archivo
npx playwright test contraste   # contraste WCAG AA en los 4 modos de tema
npx playwright test hover       # contraste de los estados :hover (claro y oscuro)
npx playwright test foco       # anillo de foco por teclado (WCAG 2.4.7)
npx playwright test redes     # validación del formulario de publicación social
```

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
