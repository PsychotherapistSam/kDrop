package de.sam.base.pages

val displayLoader = """on htmx:beforeRequest add .loading to me
                       on htmx:beforeOnLoad remove .loading from me"""