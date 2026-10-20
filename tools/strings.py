#!/usr/bin/env python3
"""Writes app/src/main/res/values*/strings.xml for the six languages from the tables below."""
import os
from xml.sax.saxutils import escape

LANGS = ["en", "fr", "de", "es", "pt", "ru"]
S = {
 "app_title": ["motivation", "motivation", "Motivation", "motivación", "motivação", "мотивация"],
 "action_ok": ["ok", "ok", "ok", "ok", "ok", "ок"],
 "action_cancel": ["cancel", "annuler", "abbrechen", "cancelar", "cancelar", "отмена"],
 "settings": ["settings", "réglages", "Einstellungen", "ajustes", "definições", "настройки"],
 "today": ["today", "aujourd'hui", "heute", "hoy", "hoje", "сегодня"],
 "on": ["on", "activé", "ein", "activado", "ligado", "вкл."],
 "off": ["off", "désactivé", "aus", "desactivado", "desligado", "выкл."],
 "pair": ["%1$s · %2$s", "%1$s · %2$s", "%1$s · %2$s", "%1$s · %2$s", "%1$s · %2$s", "%1$s · %2$s"],
 # home
 "no_habits": ["Count the days, and the urges you beat.", "Compter les jours, et les envies surmontées.", "Die Tage zählen, und die überstandenen Gelüste.", "Contar los días, y las ganas vencidas.", "Contar os dias, e as vontades vencidas.", "Считать дни и побеждённые желания."],
 "intro_text": ["Something to stop, or something to do every day. The app counts the days held and every urge you beat. A slip costs one day and nothing else: what you earned stays earned. Nothing leaves the phone.",
                "Quelque chose à arrêter, ou à faire chaque jour. L'app compte les jours tenus et chaque envie surmontée. Un écart coûte un jour, rien d'autre : ce qui est acquis reste acquis. Rien ne quitte le téléphone.",
                "Etwas aufhören, oder etwas jeden Tag tun. Die App zählt die durchgehaltenen Tage und jedes überstandene Gelüst. Ein Ausrutscher kostet einen Tag, sonst nichts: was erreicht ist, bleibt. Nichts verlässt das Telefon.",
                "Algo que dejar, o algo que hacer cada día. La app cuenta los días mantenidos y cada ganas vencidas. Un desliz cuesta un día y nada más: lo ganado queda ganado. Nada sale del teléfono.",
                "Algo para deixar, ou para fazer todos os dias. A app conta os dias mantidos e cada vontade vencida. Um deslize custa um dia e nada mais: o que foi ganho fica ganho. Nada sai do telefone.",
                "Что-то бросить или что-то делать каждый день. Приложение считает выдержанные дни и каждое побеждённое желание. Срыв стоит один день и ничего больше: достигнутое остаётся. Ничто не покидает телефон."],
 "new_habit": ["a new habit", "une nouvelle habitude", "eine neue Gewohnheit", "un nuevo hábito", "um novo hábito", "новая привычка"],
 # kinds and names
 "kind_stop": ["stop doing it", "arrêter", "damit aufhören", "dejarlo", "deixar", "бросить"],
 "kind_do": ["do it every day", "le faire chaque jour", "es jeden Tag tun", "hacerlo cada día", "fazê-lo todos os dias", "делать каждый день"],
 "kind_title": ["stop, or do", "arrêter, ou faire", "aufhören oder tun", "dejar, o hacer", "deixar, ou fazer", "бросить или делать"],
 "name_title": ["what", "quoi", "was", "qué", "o quê", "что"],
 "name_prompt_stop": ["what do you stop? (one or two words)", "qu'arrêtez-vous ? (un ou deux mots)", "womit hören Sie auf? (ein, zwei Wörter)", "¿qué deja? (una o dos palabras)", "o que deixa? (uma ou duas palavras)", "что вы бросаете? (одно-два слова)"],
 "name_prompt_do": ["what do you do every day? (one or two words)", "que faites-vous chaque jour ? (un ou deux mots)", "was tun Sie jeden Tag? (ein, zwei Wörter)", "¿qué hace cada día? (una o dos palabras)", "o que faz todos os dias? (uma ou duas palavras)", "что вы делаете каждый день? (одно-два слова)"],
 "sugg_sugar": ["sugar", "le sucre", "Zucker", "el azúcar", "o açúcar", "сахар"],
 "sugg_cigarettes": ["cigarettes", "la cigarette", "Zigaretten", "el tabaco", "o tabaco", "сигареты"],
 "sugg_alcohol": ["alcohol", "l'alcool", "Alkohol", "el alcohol", "o álcool", "алкоголь"],
 "sugg_screens": ["screens in the evening", "les écrans le soir", "Bildschirme am Abend", "las pantallas por la noche", "os ecrãs à noite", "экраны по вечерам"],
 "sugg_other": ["something else…", "autre chose…", "etwas anderes…", "otra cosa…", "outra coisa…", "другое…"],
 # start
 "start_title": ["start", "début", "Beginn", "inicio", "início", "начало"],
 "start_today": ["today", "aujourd'hui", "heute", "hoy", "hoje", "сегодня"],
 "start_monday": ["next Monday", "lundi prochain", "nächsten Montag", "el lunes que viene", "na próxima segunda", "в следующий понедельник"],
 "start_first": ["the first of next month", "le premier du mois prochain", "am Ersten des nächsten Monats", "el primero del mes que viene", "no primeiro do próximo mês", "первого числа следующего месяца"],
 "start_date": ["another date…", "une autre date…", "ein anderes Datum…", "otra fecha…", "outra data…", "другая дата…"],
 "start_date_prompt": ["a date (year-month-day)", "une date (année-mois-jour)", "ein Datum (Jahr-Monat-Tag)", "una fecha (año-mes-día)", "uma data (ano-mês-dia)", "дата (год-месяц-день)"],
 "start_hint": ["A fresh start carries further from a date that marks one: a Monday, the first of the month. Today is one too.", "Un nouveau départ porte plus loin depuis une date qui en marque un : un lundi, le premier du mois. Aujourd'hui en est une aussi.", "Ein Neuanfang trägt weiter von einem Datum, das einen markiert: ein Montag, der Erste des Monats. Heute ist auch eines.", "Un nuevo comienzo llega más lejos desde una fecha que lo marca: un lunes, el primero de mes. Hoy también lo es.", "Um novo começo vai mais longe a partir de uma data que o marca: uma segunda-feira, o primeiro do mês. Hoje também é uma.", "Новое начало крепче, если оно привязано к дате: понедельник, первое число. Сегодня тоже подходит."],
 # why and plan
 "why_title": ["why", "pourquoi", "warum", "por qué", "porquê", "зачем"],
 "why_prompt": ["why this, in your own words", "pourquoi, dans vos mots", "warum, in Ihren Worten", "por qué, con sus palabras", "porquê, nas suas palavras", "зачем, своими словами"],
 "why_hint": ["Shown when an urge comes.", "Affiché quand une envie vient.", "Wird gezeigt, wenn ein Gelüst kommt.", "Se muestra cuando vienen las ganas.", "Mostrado quando a vontade vem.", "Показывается, когда приходит желание."],
 "plan_title": ["when the urge comes, I…", "quand l'envie vient, je…", "wenn das Gelüst kommt, dann…", "cuando vienen las ganas, yo…", "quando a vontade vem, eu…", "когда приходит желание, я…"],
 "plan_prompt": ["when the urge comes, I… (a glass of water, go outside, call someone)", "quand l'envie vient, je… (un verre d'eau, sortir, appeler quelqu'un)", "wenn das Gelüst kommt, dann… (ein Glas Wasser, rausgehen, jemanden anrufen)", "cuando vienen las ganas, yo… (un vaso de agua, salir, llamar a alguien)", "quando a vontade vem, eu… (um copo de água, sair, ligar a alguém)", "когда приходит желание, я… (стакан воды, выйти на улицу, позвонить кому-то)"],
 "plan_hint": ["Decided in advance, the plan is there before the urge is.", "Décidé à l'avance, le plan est là avant l'envie.", "Im Voraus entschieden, ist der Plan da, bevor das Gelüst kommt.", "Decidido de antemano, el plan está ahí antes que las ganas.", "Decidido de antemão, o plano está lá antes da vontade.", "Решённый заранее, план уже есть, когда приходит желание."],
 # units
 "per_day_title": ["per day, before", "par jour, avant", "pro Tag, vorher", "al día, antes", "por dia, antes", "в день, раньше"],
 "per_day_prompt": ["how many a day, before?", "combien par jour, avant ?", "wie viele pro Tag, vorher?", "¿cuántos al día, antes?", "quantos por dia, antes?", "сколько в день раньше?"],
 "unit_title": ["unit", "unité", "Einheit", "unidad", "unidade", "единица"],
 "unit_prompt": ["the unit (cigarettes, drinks, pieces…)", "l'unité (cigarettes, verres, morceaux…)", "die Einheit (Zigaretten, Gläser, Stücke…)", "la unidad (cigarrillos, copas, piezas…)", "a unidade (cigarros, copos, pedaços…)", "единица (сигареты, бокалы, кусочки…)"],
 "cost_title": ["price of one", "prix d'une unité", "Preis pro Einheit", "precio de una unidad", "preço de uma unidade", "цена одной"],
 "cost_prompt": ["price of one unit", "prix d'une unité", "Preis einer Einheit", "precio de una unidad", "preço de uma unidade", "цена одной единицы"],
 "currency_title": ["currency", "monnaie", "Währung", "moneda", "moeda", "валюта"],
 "currency_prompt": ["currency (CHF, €, $…)", "monnaie (CHF, €, $…)", "Währung (CHF, €, $…)", "moneda (CHF, €, $…)", "moeda (CHF, €, $…)", "валюта (CHF, €, $…)"],
 "not_counted": ["not counted", "pas compté", "nicht gezählt", "no se cuenta", "não contado", "не считается"],
 "units_hint": ["Optional: what you avoid adds up in units and in money.", "Facultatif : ce que vous évitez s'additionne en unités et en argent.", "Optional: was Sie vermeiden, summiert sich in Einheiten und Geld.", "Opcional: lo que evita se suma en unidades y en dinero.", "Opcional: o que evita soma-se em unidades e em dinheiro.", "Необязательно: то, чего вы избегаете, складывается в единицы и деньги."],
 # edit
 "save": ["save", "enregistrer", "speichern", "guardar", "guardar", "сохранить"],
 "edit": ["change", "modifier", "ändern", "cambiar", "alterar", "изменить"],
 "delete_habit": ["delete this habit", "supprimer cette habitude", "diese Gewohnheit löschen", "eliminar este hábito", "eliminar este hábito", "удалить эту привычку"],
 "delete_q": ['delete “%1$s” and everything counted?', 'supprimer « %1$s » et tout ce qui est compté ?', '„%1$s“ und alles Gezählte löschen?', '¿eliminar « %1$s » y todo lo contado?', 'eliminar « %1$s » e tudo o que foi contado?', 'удалить « %1$s » и всё подсчитанное?'],
 "delete_yes": ["delete", "supprimer", "löschen", "eliminar", "eliminar", "удалить"],
 # habit screen
 "without_since": ["without %1$s since %2$s", "sans %1$s depuis le %2$s", "ohne %1$s seit %2$s", "sin %1$s desde el %2$s", "sem %1$s desde %2$s", "без «%1$s» с %2$s"],
 "doing_since": ["%1$s, every day since %2$s", "%1$s, chaque jour depuis le %2$s", "%1$s, jeden Tag seit %2$s", "%1$s, cada día desde el %2$s", "%1$s, todos os dias desde %2$s", "%1$s, каждый день с %2$s"],
 "starts_on": ["starts on %1$s", "commence le %1$s", "beginnt am %1$s", "empieza el %1$s", "começa a %1$s", "начнётся %1$s"],
 "day_n": ["day %1$d", "jour %1$d", "Tag %1$d", "día %1$d", "dia %1$d", "день %1$d"],
 "of_total": ["out of %1$d", "sur %1$d", "von %1$d", "de %1$d", "em %1$d", "из %1$d"],
 "restart_now": ["starting again now · tomorrow is day 1", "on repart maintenant · demain, jour 1", "jetzt geht es weiter · morgen ist Tag 1", "se vuelve a empezar ahora · mañana, día 1", "recomeça-se agora · amanhã, dia 1", "начинаем заново сейчас · завтра день 1"],
 "restart_now_do": ['starting again · today is day 1 once it is done', "on repart · aujourd'hui sera le jour 1 une fois fait", 'es geht weiter · heute ist Tag 1, sobald es getan ist', 'se vuelve a empezar · hoy será el día 1 una vez hecho', 'recomeça-se · hoje será o dia 1 depois de feito', 'начинаем заново · сегодня будет день 1, как только сделано'],
 "held_of": ['%1$s out of %2$d', '%1$s sur %2$d', '%1$s von %2$d', '%1$s de %2$d', '%1$s em %2$d', '%1$s из %2$d'],
 "best_run": ["best run %1$s", "meilleure série %1$s", "längste Serie %1$s", "mejor racha %1$s", "melhor sequência %1$s", "лучшая серия %1$s"],
 "no_resisted": ["no urge beaten yet", "pas encore d'envie surmontée", "noch kein Gelüst überstanden", "ninguna ganas vencidas todavía", "nenhuma vontade vencida ainda", "пока ни одного побеждённого желания"],
 "units_avoided": ["%1$s %2$s avoided", "%1$s %2$s évités", "%1$s %2$s vermieden", "%1$s %2$s evitados", "%1$s %2$s evitados", "%1$s %2$s избегнуто"],
 "money_saved": ["%1$s %2$s not spent", "%1$s %2$s non dépensés", "%1$s %2$s nicht ausgegeben", "%1$s %2$s no gastados", "%1$s %2$s não gastos", "%1$s %2$s не потрачено"],
 "step_of": ["day %1$d of %2$s", "jour %1$d sur %2$s", "Tag %1$d von %2$s", "día %1$d de %2$s", "dia %1$d de %2$s", "день %1$d из %2$s"],
 "step_to": ["%1$s to go before %2$s", "encore %1$s avant %2$s", "noch %1$s bis %2$s", "faltan %1$s para %2$s", "faltam %1$s para %2$s", "ещё %1$s до %2$s"],
 "step_reached": ["%1$s, reached today", "%1$s, atteint aujourd'hui", "%1$s, heute erreicht", "%1$s, alcanzado hoy", "%1$s, alcançado hoje", "%1$s, достигнуто сегодня"],
 "m_1": ["one day", "un jour", "ein Tag", "un día", "um dia", "один день"],
 "m_3": ["three days", "trois jours", "drei Tage", "tres días", "três dias", "три дня"],
 "m_7": ["one week", "une semaine", "eine Woche", "una semana", "uma semana", "неделя"],
 "m_14": ["two weeks", "deux semaines", "zwei Wochen", "dos semanas", "duas semanas", "две недели"],
 "m_21": ["three weeks", "trois semaines", "drei Wochen", "tres semanas", "três semanas", "три недели"],
 "m_30": ["one month", "un mois", "ein Monat", "un mes", "um mês", "месяц"],
 "m_66": ["66 days", "66 jours", "66 Tage", "66 días", "66 dias", "66 дней"],
 "m_100": ["100 days", "100 jours", "100 Tage", "100 días", "100 dias", "100 дней"],
 "m_180": ["six months", "six mois", "sechs Monate", "seis meses", "seis meses", "полгода"],
 "m_365": ["one year", "un an", "ein Jahr", "un año", "um ano", "год"],
 "m_66_note": ["66 days: the median time a daily habit took to become automatic, in the one study that measured it.", "66 jours : le temps médian qu'une habitude quotidienne a mis à devenir automatique, dans la seule étude qui l'a mesuré.", "66 Tage: die mittlere Zeit, bis eine tägliche Gewohnheit automatisch wurde, in der einen Studie, die es gemessen hat.", "66 días: el tiempo mediano que un hábito diario tardó en volverse automático, en el único estudio que lo midió.", "66 dias: o tempo mediano que um hábito diário levou a tornar-se automático, no único estudo que o mediu.", "66 дней: медианное время, за которое ежедневная привычка стала автоматической, в единственном исследовании, где это измерили."],
 "urge_now": ["an urge, right now", "une envie, maintenant", "ein Gelüst, jetzt", "ganas, ahora mismo", "uma vontade, agora", "желание, прямо сейчас"],
 "resist_row": ["I held out", "j'ai tenu bon", "ich habe durchgehalten", "aguanté", "aguentei", "я удержался"],
 "resist_hint": ["one urge beaten, counted", "une envie surmontée, comptée", "ein Gelüst überstanden, gezählt", "unas ganas vencidas, contadas", "uma vontade vencida, contada", "желание побеждено, засчитано"],
 "done_row": ["done today", "fait aujourd'hui", "heute getan", "hecho hoy", "feito hoje", "сделано сегодня"],
 "done_already": ["done today ✓", "fait aujourd'hui ✓", "heute getan ✓", "hecho hoy ✓", "feito hoje ✓", "сделано сегодня ✓"],
 "slip_row": ["I slipped", "j'ai craqué", "ich bin schwach geworden", "caí", "cedi", "я сорвался"],
 "share_progress": ["tell someone where I am", "dire à quelqu'un où j'en suis", "jemandem sagen, wo ich stehe", "contarle a alguien dónde estoy", "dizer a alguém onde estou", "рассказать кому-то, где я"],
 "share_hint": ["Said to someone, it holds better.", "Dit à quelqu'un, ça tient mieux.", "Jemandem gesagt, hält es besser.", "Dicho a alguien, se mantiene mejor.", "Dito a alguém, aguenta-se melhor.", "Сказанное кому-то держится крепче."],
 "last_weeks": ["the last weeks", "les dernières semaines", "die letzten Wochen", "las últimas semanas", "as últimas semanas", "последние недели"],
 "legend_stop": ["■ held · □ slip", "■ tenu · □ écart", "■ durchgehalten · □ Ausrutscher", "■ mantenido · □ desliz", "■ mantido · □ deslize", "■ выдержан · □ срыв"],
 "legend_do": ['■ done · □ missed · ◦ today', "■ fait · □ manqué · ◦ aujourd'hui", '■ getan · □ verpasst · ◦ heute', '■ hecho · □ no hecho · ◦ hoy', '■ feito · □ falhado · ◦ hoje', '■ сделано · □ пропущено · ◦ сегодня'],
 # urge screen
 "urge_title": ["an urge", "une envie", "ein Gelüst", "unas ganas", "uma vontade", "желание"],
 "urge_text": ["It rises, peaks and passes, whether you feed it or not. Three minutes of breathing with the ring, and see.", "Elle monte, culmine et passe, qu'on la nourrisse ou non. Trois minutes de respiration avec l'anneau, et on voit.", "Es steigt, erreicht den Gipfel und vergeht, ob man es nährt oder nicht. Drei Minuten Atmen mit dem Ring, dann sehen wir.", "Suben, llegan a lo más alto y pasan, se alimenten o no. Tres minutos de respiración con el anillo, y a ver.", "Sobe, atinge o pico e passa, quer se alimente ou não. Três minutos a respirar com o anel, e vê-se.", "Оно нарастает, достигает пика и проходит, кормите вы его или нет. Три минуты дыхания с кольцом, и посмотрим."],
 "urge_done_text": ["Three minutes. Did it pass?", "Trois minutes. Elle a passé ?", "Drei Minuten. Ist es vorbei?", "Tres minutos. ¿Pasaron?", "Três minutos. Passou?", "Три минуты. Прошло?"],
 "urge_passed": ["it passed", "elle a passé", "es ist vorbei", "pasaron", "passou", "прошло"],
 "urge_passed_hint": ["counted as an urge beaten", "comptée comme envie surmontée", "als überstandenes Gelüst gezählt", "contadas como ganas vencidas", "contada como vontade vencida", "засчитано как побеждённое желание"],
 "urge_still": ["I slipped", "j'ai craqué", "ich bin schwach geworden", "caí", "cedi", "я сорвался"],
 "breathe_in": ["breathe in", "inspirez", "einatmen", "inspire", "inspire", "вдох"],
 "breathe_hold": ["hold", "gardez", "halten", "mantenga", "segure", "задержите"],
 "breathe_out": ["breathe out", "soufflez", "ausatmen", "suelte", "solte", "выдох"],
 # slip screen
 "slip_title": ["a slip", "un écart", "ein Ausrutscher", "un desliz", "um deslize", "срыв"],
 "slip_text": ['A slip, not a relapse. %1$s, %2$s. The next hour counts more than the last one.', "Un écart, pas une rechute. %1$s, %2$s. L'heure qui vient compte plus que celle qui passe.", 'Ein Ausrutscher, kein Rückfall. %1$s, %2$s. Die nächste Stunde zählt mehr als die letzte.', 'Un desliz, no una recaída. %1$s, %2$s. La próxima hora cuenta más que la última.', 'Um deslize, não uma recaída. %1$s, %2$s. A próxima hora conta mais do que a última.', 'Срыв, а не возврат. %1$s, %2$s. Следующий час важнее прошедшего.'],
 "slip_text_do": ['A day missed, not a habit lost. %1$s. Tomorrow is one more.', 'Un jour manqué, pas une habitude perdue. %1$s. Demain en fait un de plus.', 'Ein Tag verpasst, keine Gewohnheit verloren. %1$s. Morgen kommt einer dazu.', 'Un día perdido, no un hábito perdido. %1$s. Mañana es uno más.', 'Um dia falhado, não um hábito perdido. %1$s. Amanhã é mais um.', 'Пропущен день, а не привычка. %1$s. Завтра будет ещё один.'],
 "slip_plan": ["your plan: %1$s", "votre plan : %1$s", "Ihr Plan: %1$s", "su plan: %1$s", "o seu plano: %1$s", "ваш план: %1$s"],
 "slip_note_prompt": ["what happened? (optional)", "que s'est-il passé ? (facultatif)", "was ist passiert? (optional)", "¿qué pasó? (opcional)", "o que aconteceu? (opcional)", "что случилось? (необязательно)"],
 "slip_note_row": ["what happened…", "ce qui s'est passé…", "was passiert ist…", "lo que pasó…", "o que aconteceu…", "что случилось…"],
 "slip_confirm": ["noted, I start again now", "c'est noté, je repars maintenant", "notiert, ich fange jetzt wieder an", "anotado, vuelvo a empezar ahora", "anotado, recomeço agora", "записано, начинаю заново сейчас"],
 # record
 "journal": ["the record", "le relevé", "das Protokoll", "el registro", "o registo", "записи"],
 "journal_empty": ["nothing noted yet", "rien de noté encore", "noch nichts notiert", "nada anotado todavía", "nada anotado ainda", "пока ничего не записано"],
 "ev_resist": ["urge beaten", "envie surmontée", "Gelüst überstanden", "ganas vencidas", "vontade vencida", "желание побеждено"],
 "ev_slip": ["slip", "écart", "Ausrutscher", "desliz", "deslize", "срыв"],
 "ev_done": ["day done", "jour fait", "Tag getan", "día hecho", "dia feito", "день сделан"],
 "remove_entry": ["remove this entry", "retirer cette ligne", "diesen Eintrag entfernen", "quitar esta línea", "remover esta linha", "удалить эту запись"],
 # widget and reminder
 "widget_name": ["Reader's Motivation", "Reader's Motivation", "Reader's Motivation", "Reader's Motivation", "Reader's Motivation", "Reader's Motivation"],
 "widget_desc": ["the day and the count; ✓ counts an urge beaten", "le jour et le compte ; ✓ compte une envie surmontée", "der Tag und die Zahl; ✓ zählt ein überstandenes Gelüst", "el día y la cuenta; ✓ cuenta unas ganas vencidas", "o dia e a conta; ✓ conta uma vontade vencida", "день и счёт; ✓ засчитывает побеждённое желание"],
 "widget_habit_title": ["on the widget and in the reminder", "sur le widget et dans le rappel", "im Widget und in der Erinnerung", "en el widget y en el recordatorio", "no widget e no lembrete", "в виджете и в напоминании"],
 "reminder_title": ["a daily reminder", "un rappel quotidien", "eine tägliche Erinnerung", "un recordatorio diario", "um lembrete diário", "ежедневное напоминание"],
 "reminder_hint": ["One line at the hour you choose: the day, the count, and the question. Off unless you turn it on.", "Une ligne à l'heure choisie : le jour, le compte, et la question. Éteint tant que vous ne l'allumez pas.", "Eine Zeile zur gewählten Stunde: der Tag, die Zahl und die Frage. Aus, solange Sie es nicht einschalten.", "Una línea a la hora elegida: el día, la cuenta y la pregunta. Apagado mientras no lo active.", "Uma linha à hora escolhida: o dia, a conta e a pergunta. Desligado enquanto não o ligar.", "Одна строка в выбранный час: день, счёт и вопрос. Выключено, пока вы не включите."],
 "time_prompt": ["time (HH:MM)", "heure (HH:MM)", "Uhrzeit (HH:MM)", "hora (HH:MM)", "hora (HH:MM)", "время (ЧЧ:ММ)"],
 "notif_channel": ["reminder", "rappel", "Erinnerung", "recordatorio", "lembrete", "напоминание"],
 "notif_question_stop": ["all held today?", "tout tenu aujourd'hui ?", "heute alles durchgehalten?", "¿todo mantenido hoy?", "tudo mantido hoje?", "всё выдержано сегодня?"],
 "notif_question_do": ["done today?", "fait aujourd'hui ?", "heute getan?", "¿hecho hoy?", "feito hoje?", "сделано сегодня?"],
 # backup
 "export_backup": ["save a backup…", "enregistrer une sauvegarde…", "eine Sicherung speichern…", "guardar una copia…", "guardar uma cópia…", "сохранить резервную копию…"],
 "import_backup": ["restore a backup…", "restaurer une sauvegarde…", "eine Sicherung wiederherstellen…", "restaurar una copia…", "restaurar uma cópia…", "восстановить из копии…"],
 "export_saved": ["saved as %1$s", "enregistré sous %1$s", "gespeichert als %1$s", "guardado como %1$s", "guardado como %1$s", "сохранено как %1$s"],
 "share_export": ["share this file…", "partager ce fichier…", "diese Datei teilen…", "compartir este archivo…", "partilhar este ficheiro…", "поделиться этим файлом…"],
 "import_done": ["backup restored", "sauvegarde restaurée", "Sicherung wiederhergestellt", "copia restaurada", "cópia restaurada", "копия восстановлена"],
 "import_failed": ["this file is not a backup of this app", "ce fichier n'est pas une sauvegarde de cette app", "diese Datei ist keine Sicherung dieser App", "este archivo no es una copia de esta app", "este ficheiro não é uma cópia desta app", "этот файл не является копией этого приложения"],
 "import_q": ["replace everything by this backup?", "tout remplacer par cette sauvegarde ?", "alles durch diese Sicherung ersetzen?", "¿reemplazar todo por esta copia?", "substituir tudo por esta cópia?", "заменить всё этой копией?"],
 "replace": ["replace", "remplacer", "ersetzen", "reemplazar", "substituir", "заменить"],
 # look
 "theme_dark": ["dark", "sombre", "dunkel", "oscuro", "escuro", "тёмная"],
 "theme_light": ["light", "clair", "hell", "claro", "claro", "светлая"],
 "colours": ["colours", "couleurs", "Farben", "colores", "cores", "цвета"],
 "text_size": ["text size", "taille du texte", "Textgröße", "tamaño del texto", "tamanho do texto", "размер текста"],
 "size_small": ["small", "petite", "klein", "pequeño", "pequeno", "мелкий"],
 "size_medium": ["medium", "moyenne", "mittel", "mediano", "médio", "средний"],
 "size_large": ["large", "grande", "groß", "grande", "grande", "крупный"],
 "font": ["font", "police", "Schrift", "fuente", "letra", "шрифт"],
 "font_sans": ["sans serif", "sans empattement", "serifenlos", "sin serifa", "sem serifa", "без засечек"],
 "font_serif": ["serif", "avec empattement", "mit Serifen", "con serifa", "com serifa", "с засечками"],
 "font_mono": ["monospace", "à chasse fixe", "Festbreite", "monoespaciada", "monoespaçada", "моноширинный"],
 "haptics": ["vibration on tap", "vibration au toucher", "Vibration beim Tippen", "vibración al tocar", "vibração ao tocar", "вибрация при нажатии"],
 "about": ["about", "à propos", "über", "acerca de", "sobre", "о приложении"],
 "credits": ["written with Claude Code · nothing leaves the phone", "écrit avec Claude Code · rien ne quitte le téléphone", "geschrieben mit Claude Code · nichts verlässt das Telefon", "escrito con Claude Code · nada sale del teléfono", "escrito com Claude Code · nada sai do telefone", "написано с Claude Code · ничто не покидает телефон"],
 "why_it_works": ["why it is built this way", "pourquoi elle est faite ainsi", "warum sie so gebaut ist", "por qué está hecha así", "porque está feita assim", "почему оно устроено так"],
 "why_it_works_text": ["Writing a count down, and telling someone, makes a goal hold better (138 experiments). A streak motivates, but a broken streak shown as a loss makes people give up, and after a slip it is the lost confidence, not the guilt, that leads to relapse: so a slip costs one day and the totals stay. Each urge beaten is proof you can. An urge peaks and passes, and keeping the mind busy for a few minutes lowers it. A plan decided in advance roughly doubles the odds of following it. A start on a Monday or a first of the month carries further. The sources are in the app's repository.",
                      "Noter un compte, et le dire à quelqu'un, fait mieux tenir un but (138 expériences). Une série motive, mais une série cassée montrée comme une perte fait lâcher, et après un écart c'est la confiance perdue, pas la culpabilité, qui mène à la rechute : un écart coûte donc un jour et les totaux restent. Chaque envie surmontée est une preuve que vous pouvez. Une envie culmine et passe, et occuper l'esprit quelques minutes la fait baisser. Un plan décidé à l'avance double à peu près les chances de le suivre. Un départ un lundi ou un premier du mois porte plus loin. Les sources sont dans le dépôt de l'app.",
                      "Eine Zahl aufzuschreiben und jemandem zu sagen, lässt ein Ziel besser halten (138 Experimente). Eine Serie motiviert, aber eine gebrochene Serie als Verlust gezeigt lässt aufgeben, und nach einem Ausrutscher führt das verlorene Vertrauen, nicht die Schuld, zum Rückfall: darum kostet ein Ausrutscher einen Tag, und die Summen bleiben. Jedes überstandene Gelüst ist ein Beweis, dass Sie es können. Ein Gelüst erreicht seinen Gipfel und vergeht, und den Kopf ein paar Minuten zu beschäftigen senkt es. Ein im Voraus gefasster Plan verdoppelt etwa die Chance, ihn zu befolgen. Ein Beginn an einem Montag oder Monatsersten trägt weiter. Die Quellen stehen im Repository der App.",
                      "Anotar una cuenta, y contársela a alguien, hace que un objetivo se mantenga mejor (138 experimentos). Una racha motiva, pero una racha rota mostrada como pérdida hace abandonar, y tras un desliz es la confianza perdida, no la culpa, lo que lleva a la recaída: por eso un desliz cuesta un día y los totales quedan. Cada ganas vencidas prueban que puede. Las ganas llegan a lo más alto y pasan, y ocupar la mente unos minutos las baja. Un plan decidido de antemano duplica más o menos las posibilidades de seguirlo. Empezar un lunes o un primero de mes llega más lejos. Las fuentes están en el repositorio de la app.",
                      "Anotar uma conta, e dizê-la a alguém, faz um objetivo aguentar melhor (138 experiências). Uma sequência motiva, mas uma sequência quebrada mostrada como perda faz desistir, e depois de um deslize é a confiança perdida, não a culpa, que leva à recaída: por isso um deslize custa um dia e os totais ficam. Cada vontade vencida é prova de que consegue. Uma vontade atinge o pico e passa, e ocupar a mente alguns minutos fá-la baixar. Um plano decidido de antemão duplica mais ou menos as hipóteses de o seguir. Começar numa segunda-feira ou no primeiro do mês vai mais longe. As fontes estão no repositório da app.",
                      "Записанный счёт и рассказ кому-то помогают держать цель (138 экспериментов). Серия мотивирует, но показанная как потеря сорванная серия заставляет бросить, а после срыва к возврату ведёт потерянная уверенность, не вина: поэтому срыв стоит один день, а итоги остаются. Каждое побеждённое желание — доказательство, что вы можете. Желание достигает пика и проходит, а занятый на несколько минут ум его ослабляет. Решённый заранее план примерно удваивает шансы ему последовать. Начало в понедельник или первого числа крепче. Источники — в репозитории приложения."],
}

# plurals: en/fr/de/es/pt use one/other; ru uses one/few/many (other = many)
P = {
 "days": [{"one": "%d day", "other": "%d days"}, {"one": "%d jour", "other": "%d jours"}, {"one": "%d Tag", "other": "%d Tage"}, {"one": "%d día", "other": "%d días"}, {"one": "%d dia", "other": "%d dias"}, {"one": "%d день", "few": "%d дня", "many": "%d дней", "other": "%d дней"}],
 "urges": [{"one": "%d urge beaten", "other": "%d urges beaten"}, {"one": "%d envie surmontée", "other": "%d envies surmontées"}, {"one": "%d Gelüst überstanden", "other": "%d Gelüste überstanden"}, {"one": "%d ganas vencidas", "other": "%d ganas vencidas"}, {"one": "%d vontade vencida", "other": "%d vontades vencidas"}, {"one": "%d желание побеждено", "few": "%d желания побеждено", "many": "%d желаний побеждено", "other": "%d желаний побеждено"}],
 "years": [{"one": "%d year", "other": "%d years"}, {"one": "%d an", "other": "%d ans"}, {"one": "%d Jahr", "other": "%d Jahre"}, {"one": "%d año", "other": "%d años"}, {"one": "%d ano", "other": "%d anos"}, {"one": "%d год", "few": "%d года", "many": "%d лет", "other": "%d лет"}],
 "days_held": [{"one": "%d day held", "other": "%d days held"}, {"one": "%d jour tenu", "other": "%d jours tenus"}, {"one": "%d Tag durchgehalten", "other": "%d Tage durchgehalten"}, {"one": "%d día mantenido", "other": "%d días mantenidos"}, {"one": "%d dia mantido", "other": "%d dias mantidos"}, {"one": "%d день выдержан", "few": "%d дня выдержано", "many": "%d дней выдержано", "other": "%d дней выдержано"}],
 "days_done": [{"one": "%d day done", "other": "%d days done"}, {"one": "%d jour fait", "other": "%d jours faits"}, {"one": "%d Tag getan", "other": "%d Tage getan"}, {"one": "%d día hecho", "other": "%d días hechos"}, {"one": "%d dia feito", "other": "%d dias feitos"}, {"one": "%d день сделан", "few": "%d дня сделано", "many": "%d дней сделано", "other": "%d дней сделано"}],
 "held_stay": [{"one": "%d day held stays held", "other": "%d days held stay held"}, {"one": "%d jour tenu reste tenu", "other": "%d jours tenus restent tenus"}, {"one": "%d Tag bleibt durchgehalten", "other": "%d Tage bleiben durchgehalten"}, {"one": "%d día mantenido sigue mantenido", "other": "%d días mantenidos siguen mantenidos"}, {"one": "%d dia mantido fica mantido", "other": "%d dias mantidos ficam mantidos"}, {"one": "%d день остаётся выдержанным", "few": "%d дня остаются выдержанными", "many": "%d дней остаются выдержанными", "other": "%d дней остаются выдержанными"}],
 "urges_stay": [{"one": "%d urge beaten stays beaten", "other": "%d urges beaten stay beaten"}, {"one": "%d envie surmontée reste surmontée", "other": "%d envies surmontées restent surmontées"}, {"one": "%d Gelüst bleibt überstanden", "other": "%d Gelüste bleiben überstanden"}, {"one": "%d ganas vencidas siguen vencidas", "other": "%d ganas vencidas siguen vencidas"}, {"one": "%d vontade vencida fica vencida", "other": "%d vontades vencidas ficam vencidas"}, {"one": "%d желание остаётся побеждённым", "few": "%d желания остаются побеждёнными", "many": "%d желаний остаются побеждёнными", "other": "%d желаний остаются побеждёнными"}],
 "done_stay": [{"one": "%d day done stays done", "other": "%d days done stay done"}, {"one": "%d jour fait reste fait", "other": "%d jours faits restent faits"}, {"one": "%d Tag bleibt getan", "other": "%d Tage bleiben getan"}, {"one": "%d día hecho sigue hecho", "other": "%d días hechos siguen hechos"}, {"one": "%d dia feito fica feito", "other": "%d dias feitos ficam feitos"}, {"one": "%d день остаётся сделанным", "few": "%d дня остаются сделанными", "many": "%d дней остаются сделанными", "other": "%d дней остаются сделанными"}],
}

def xml_text(s):
    return escape(s).replace("'", "\\'")

root = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res")
for i, lang in enumerate(LANGS):
    d = os.path.join(root, "values" if lang == "en" else "values-" + lang)
    os.makedirs(d, exist_ok=True)
    lines = ['<?xml version="1.0" encoding="utf-8"?>', '<resources>']
    if lang == "en":
        lines.append('    <string name="app_name">Reader\\\'s Motivation</string>')
    for k, v in S.items():
        if lang == "en" and k == "app_name": continue
        lines.append('    <string name="%s">%s</string>' % (k, xml_text(v[i])))
    for k, v in P.items():
        lines.append('    <plurals name="%s">' % k)
        for q, t in v[i].items():
            lines.append('        <item quantity="%s">%s</item>' % (q, xml_text(t)))
        lines.append('    </plurals>')
    lines.append('</resources>')
    with open(os.path.join(d, "strings.xml"), "w") as f:
        f.write("\n".join(lines) + "\n")
    print(lang, len(S) + len(P))
