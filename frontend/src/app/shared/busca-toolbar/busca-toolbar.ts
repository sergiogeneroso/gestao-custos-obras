import { Component, input, model } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

@Component({
  selector: 'app-busca-toolbar',
  imports: [FormsModule, MatFormFieldModule, MatInputModule],
  templateUrl: './busca-toolbar.html',
  styleUrl: './busca-toolbar.scss',
})
export class BuscaToolbar {
  readonly valor = model('');
  readonly rotulo = input('Buscar');
  readonly placeholder = input('');
}
