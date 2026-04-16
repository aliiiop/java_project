package com.bot;
import java.util.*;
public class RatingUtils {
    public static double evaluateFace(String photoId, String gender) {
        if (photoId == null || photoId.isEmpty()) return 0;
        // Генерируем стабильную оценку на основе хеша photoId
        int hash = photoId.hashCode();
        double rating = 3.5 + (Math.abs(hash % 50) / 10.0);
        return Math.max(1.0, Math.min(10.0, rating));
    }
    public static double evaluateBodyByPhoto(String photoId, String gender) {
        if (photoId == null || photoId.isEmpty()) return 0;
        // Генерируем стабильную оценку на основе хеша photoId
        int hash = photoId.hashCode();
        double rating = 3.0 + (Math.abs(hash % 60) / 10.0);
        return Math.max(1.0, Math.min(10.0, rating));
    }
    public static double evaluateBodyByMeasurements(Map<String, Double> m, String gender) {
        // Получаем основные параметры
        double height = m.getOrDefault("height", 175.0); // см
        double weight = m.getOrDefault("weight", 75.0);  // кг
        double chest = m.getOrDefault("chest", 100.0);
        double waist = m.getOrDefault("waist", 80.0);
        double hips = m.getOrDefault("hips", 90.0);
        double shoulder = m.getOrDefault("shoulder", 45.0);
        double neck = m.getOrDefault("neck", 38.0);
        
        double rating = 5.0; // базовая оценка
        
        if (gender.equals("male")) {
            // === МУЖЧИНЫ ===
            // 1. BMI анализ (20-25 идеально)
            double bmi = weight / ((height / 100.0) * (height / 100.0));
            if (bmi >= 18.5 && bmi <= 25.0) {
                rating += 1.5;
            } else if (bmi >= 25.0 && bmi <= 27.0) {
                rating += 0.8;
            } else if (bmi >= 17.0 && bmi < 18.5) {
                rating += 0.5;
            } else {
                rating -= Math.abs(bmi - 23.0) * 0.3;
            }
            
            // 2. Плечи относительно талии (идеально 1.5-1.7)
            double shoulderWaistRatio = shoulder / waist;
            if (shoulderWaistRatio >= 1.5 && shoulderWaistRatio <= 1.7) {
                rating += 1.5;
            } else if (shoulderWaistRatio >= 1.4 && shoulderWaistRatio <= 1.8) {
                rating += 0.8;
            } else {
                rating -= Math.abs(shoulderWaistRatio - 1.6) * 0.8;
            }
            
            // 3. Грудь-талия соотношение (идеально 1.25-1.35)
            double chestWaistRatio = chest / waist;
            if (chestWaistRatio >= 1.20 && chestWaistRatio <= 1.35) {
                rating += 1.2;
            } else {
                rating -= Math.abs(chestWaistRatio - 1.27) * 0.5;
            }
            
            // 4. Рост (175-185 идеально)
            if (height >= 175 && height <= 185) {
                rating += 1.0;
            } else if (height >= 170 && height < 175) {
                rating += 0.5;
            } else if (height > 185 && height <= 195) {
                rating += 0.3;
            } else {
                rating -= Math.abs(height - 177.0) * 0.04;
            }
            
            // 5. Шея (40-42 см идеально)
            if (neck >= 39 && neck <= 43) {
                rating += 0.8;
            } else {
                rating -= Math.abs(neck - 41.0) * 0.1;
            }
            
        } else {
            // === ЖЕНЩИНЫ ===
            // 1. BMI анализ (19-24 идеально)
            double bmi = weight / ((height / 100.0) * (height / 100.0));
            if (bmi >= 19.0 && bmi <= 24.0) {
                rating += 1.5;
            } else if (bmi >= 24.0 && bmi <= 26.0) {
                rating += 0.7;
            } else if (bmi >= 18.0 && bmi < 19.0) {
                rating += 0.8;
            } else {
                rating -= Math.abs(bmi - 21.5) * 0.3;
            }
            
            // 2. Талия-бедра соотношение (идеально 0.65-0.75)
            double waistHipsRatio = waist / hips;
            if (waistHipsRatio >= 0.65 && waistHipsRatio <= 0.75) {
                rating += 2.0;
            } else if (waistHipsRatio >= 0.60 && waistHipsRatio <= 0.80) {
                rating += 1.0;
            } else {
                rating -= Math.abs(waistHipsRatio - 0.70) * 1.5;
            }
            
            // 3. Грудь-талия соотношение (идеально 0.85-0.95)
            double chestWaistRatio = chest / waist;
            if (chestWaistRatio >= 0.85 && chestWaistRatio <= 0.95) {
                rating += 1.2;
            } else {
                rating -= Math.abs(chestWaistRatio - 0.90) * 0.6;
            }
            
            // 4. Рост (162-175 идеально)
            if (height >= 162 && height <= 175) {
                rating += 1.0;
            } else if (height >= 158 && height < 162) {
                rating += 0.4;
            } else if (height > 175 && height <= 183) {
                rating += 0.6;
            } else {
                rating -= Math.abs(height - 168.0) * 0.03;
            }
            
            // 5. Объём груди (85-95 см идеально)
            if (chest >= 85 && chest <= 95) {
                rating += 0.8;
            } else {
                rating -= Math.abs(chest - 90.0) * 0.08;
            }
        }
        
        return Math.max(1.0, Math.min(10.0, rating));
    }
    public static String generateRatingMessage(double rating, boolean premium, String gender) {
        StringBuilder msg = new StringBuilder();
        msg.append("🌟 *ВАШ РЕЙТИНГ: ").append(String.format("%.1f", rating)).append("/10 PSL* 🌟\n\n");

        if (!premium) {
            // === БЕСПЛАТНАЯ ВЕРСИЯ ===
            msg.append("📈 *Потенциал:* ").append(rating < 4 ? "Низкий" : rating < 7 ? "Средний" : "Высокий").append("\n");
            msg.append(rating < 3.5 ? "\n🎭 *ЭДИТ: ТЕБЯ МОГГАЮТ* 👎" : rating <= 5.5 ? "\n🎭 *ЭДИТ: НЕЙТРАЛЬНО* 😐" : "\n🎭 *ЭДИТ: ТЫ МОГГАЕШЬ!* 🔥");
            if (rating < 4) msg.append("\n\n⚪ *WHITE PILL* ⚪\nВнешность не главное. Развивайся! 💪");
            msg.append("\n\n━━━━━━━━━━━━━━━━━━━\n");
            msg.append("🔒 *Хочешь подробный разбор?*\n");
            msg.append("💎 Нажми «Премиум» для:\n");
            msg.append("• Детального анализа лица/тела\n");
            msg.append("• Персональных рекомендаций\n");
            msg.append("• Плана улучшения внешности\n");
        } else {
            // === ПРЕМИУМ ВЕРСИЯ ===
            msg.append("═══════════════════════════════\n");
            msg.append("💎 *ПРЕМИУМ АНАЛИЗ*\n");
            msg.append("═══════════════════════════════\n\n");

            if (rating < 3.5) {
                msg.append("📊 *УРОВЕНЬ: ТРЕБУЕТ РАБОТЫ*\n\n");
                msg.append("🔍 *ДЕТАЛЬНЫЙ РАЗБОР:*\n");
                msg.append("Твой профиль сейчас ниже среднего.\n");
                msg.append("Но это НЕ приговор — это отправная точка!\n\n");
                msg.append("🎯 *ПЛАН ДЕЙСТВИЙ:*\n\n");
                msg.append("1️⃣ *Физическая форма:*\n");
                msg.append("   • Тренировки 4-5 раз в неделю\n");
                msg.append("   • Силовые для мышечного корсета\n");
                msg.append("   • Кардио 3 раза для рельефа\n\n");
                msg.append("2️⃣ *Питание:*\n");
                msg.append("   • Белки: 1.5-2г на кг веса\n");
                msg.append("   • Дефицит калорий для сушки\n");
                msg.append("   • Вода: 2-3л в день\n\n");
                msg.append("3️⃣ *Внешний вид:*\n");
                msg.append("   • Уход за кожей (очищение + увлажнение)\n");
                msg.append("   • Стильная причёска под тип лица\n");
                msg.append("   • Подбор гардероба\n\n");
                msg.append("⏱ *Срок улучшения: 3-6 месяцев*\n");
                msg.append("📈 *Потенциал: до 5.5-6.5*\n");
            } else if (rating < 5.5) {
                msg.append("📊 *УРОВЕНЬ: СРЕДНИЙ*\n\n");
                msg.append("🔍 *ДЕТАЛЬНЫЙ РАЗБОР:*\n");
                msg.append("Хорошая база! У тебя есть потенциал.\n\n");
                msg.append("🎯 *ПЛАН УЛУЧШЕНИЯ:*\n\n");
                msg.append("1️⃣ *Мышечная масса:*\n");
                msg.append("   • Гипертрофия: 3-4 тренировки в неделю\n");
                msg.append("   • Прогрессивная перегрузка\n");
                msg.append("   • Фокус на плечи и спину\n\n");
                msg.append("2️⃣ *Оптимизация пропорций:*\n");
                msg.append("   • Снижение % жира до 12-15%\n");
                msg.append("   • Улучшение осанки\n");
                msg.append("   • Работа над V-образным силуэтом\n\n");
                msg.append("3️⃣ *Стиль и имидж:*\n");
                msg.append("   • Качественная одежда под тип фигуры\n");
                msg.append("   • Уход за кожей и волосами\n");
                msg.append("   • Аксессуары и детали\n\n");
                msg.append("⏱ *Срок улучшения: 3-4 месяца*\n");
                msg.append("📈 *Потенциал: до 7.0-8.0*\n");
            } else if (rating < 8.0) {
                msg.append("📊 *УРОВЕНЬ: ВЫШЕ СРЕДНЕГО* 💪\n\n");
                msg.append("🔍 *ДЕТАЛЬНЫЙ РАЗБОР:*\n");
                msg.append("Отличная форма! Ты уже впереди большинства.\n\n");
                msg.append("🎯 *ТОНКАЯ НАСТРОЙКА:*\n\n");
                msg.append("1️⃣ *Детализация тела:*\n");
                msg.append("   • Специализированные тренировки\n");
                msg.append("   • Работа над слабыми группами мышц\n");
                msg.append("   • Снижение жира до 8-12%\n\n");
                msg.append("2️⃣ *Максимизация внешности:*\n");
                msg.append("   • Профессиональный уход за кожей\n");
                msg.append("   • Оптимальная причёска\n");
                msg.append("   • Премиальный стиль одежды\n\n");
                msg.append("3️⃣ *Харизма:*\n");
                msg.append("   • Уверенная осанка и походка\n");
                msg.append("   • Язык тела\n");
                msg.append("   • Социальные навыки\n\n");
                msg.append("⏱ *Срок до элиты: 2-3 месяца*\n");
                msg.append("📈 *Потенциал: до 8.5-9.0*\n");
            } else {
                msg.append("📊 *УРОВЕНЬ: ЭЛИТ* 👑\n\n");
                msg.append("🔍 *ДЕТАЛЬНЫЙ РАЗБОР:*\n");
                msg.append("Ты в топ 1%! Поздравляем!\n\n");
                msg.append("🎯 *ПОДДЕРЖАНИЕ УРОВНЯ:*\n\n");
                msg.append("1️⃣ *Консистентность:*\n");
                msg.append("   • Регулярные тренировки\n");
                msg.append("   • Дисциплина в питании\n");
                msg.append("   • 7-8 часов сна\n\n");
                msg.append("2️⃣ *Совершенствование:*\n");
                msg.append("   • Инновационные методы тренировок\n");
                msg.append("   • Персональный подход к питанию\n");
                msg.append("   • Биохакинг и восстановление\n\n");
                msg.append("3️⃣ *Личный бренд:*\n");
                msg.append("   • Уникальный стиль\n");
                msg.append("   • Уверенность и харизма\n");
                msg.append("   • Премиум имидж\n\n");
                msg.append("✨ *Ты МОГГЕР! Держи этот уровень!* ✨\n");
            }

            msg.append("\n═══════════════════════════════\n");
        }

        return msg.toString();
    }
    public static String generateMeasurementsMessage(Map<String, Double> measurements, double rating, boolean premium, String gender) {
        double height = measurements.getOrDefault("height", 175.0);
        double weight = measurements.getOrDefault("weight", 75.0);
        double chest = measurements.getOrDefault("chest", 100.0);
        double waist = measurements.getOrDefault("waist", 80.0);
        double hips = measurements.getOrDefault("hips", 90.0);
        double shoulder = measurements.getOrDefault("shoulder", 45.0);
        double neck = measurements.getOrDefault("neck", 38.0);

        double bmi = weight / ((height / 100.0) * (height / 100.0));
        StringBuilder msg = new StringBuilder();
        msg.append("🌟 *АНАЛИЗ ПО ЗАМЕРАМ: ").append(String.format("%.1f", rating)).append("/10 PSL* 🌟\n\n");

        if (!premium) {
            // === БЕСПЛАТНАЯ ВЕРСИЯ — только базовые параметры ===
            msg.append("📊 *ТВОИ ПАРАМЕТРЫ:*\n");
            msg.append("• Рост: ").append(String.format("%.0f", height)).append(" см\n");
            msg.append("• Вес: ").append(String.format("%.1f", weight)).append(" кг\n");
            msg.append("• BMI: ").append(String.format("%.1f", bmi)).append("\n\n");

            msg.append("🎯 *ИТОГ:*\n");
            if (rating < 3.5) msg.append("🔴 *УРОВЕНЬ: ТРЕБУЕТ РАБОТЫ* 👎\n");
            else if (rating < 5.5) msg.append("🟡 *УРОВЕНЬ: СРЕДНИЙ* 😐\n");
            else if (rating < 8.0) msg.append("🟢 *УРОВЕНЬ: ХОРОШИЙ* 💪\n");
            else msg.append("🟢 *УРОВЕНЬ: ЭЛИТ* 👑\n");

            msg.append("\n━━━━━━━━━━━━━━━━━━━\n");
            msg.append("🔒 *Хочешь полный разбор?*\n");
            msg.append("💎 Нажми «Премиум» чтобы увидеть:\n");
            msg.append("• Все соотношения с оценкой ✅/⚠️\n");
            msg.append("• Какие мышцы качать\n");
            msg.append("• Персональный план тренировок\n");
            msg.append("• Рекомендации по питанию\n");
        } else {
            // === ПРЕМИУМ ВЕРСИЯ — полный анализ ===
            msg.append("═══════════════════════════════\n");
            msg.append("💎 *ПРЕМИУМ АНАЛИЗ ЗАМЕРОВ*\n");
            msg.append("═══════════════════════════════\n\n");

            if (gender.equals("male")) {
                msg.append("📏 *ТВОИ ПАРАМЕТРЫ:*\n");
                msg.append("• Рост: ").append(String.format("%.0f", height)).append(" см ").append(height >= 175 && height <= 185 ? "✅ идеально" : "⚠️").append("\n");
                msg.append("• Вес: ").append(String.format("%.1f", weight)).append(" кг\n");
                msg.append("• BMI: ").append(String.format("%.1f", bmi)).append(" ").append(bmi >= 18.5 && bmi <= 25.0 ? "✅ норма" : bmi < 18.5 ? "⚠️ недовес" : "⚠️ перевес").append("\n");
                msg.append("• Плечи: ").append(String.format("%.0f", shoulder)).append(" см\n");
                msg.append("• Грудь: ").append(String.format("%.0f", chest)).append(" см\n");
                msg.append("• Талия: ").append(String.format("%.0f", waist)).append(" см\n");
                msg.append("• Шея: ").append(String.format("%.0f", neck)).append(" см\n\n");

                double swr = shoulder / waist;
                double cwr = chest / waist;
                msg.append("💪 *КЛЮЧЕВЫЕ СООТНОШЕНИЯ:*\n");
                msg.append("• Плечи/Талия: ").append(String.format("%.2f", swr)).append(" ").append(swr >= 1.5 && swr <= 1.7 ? "✅ V-образный силуэт!" : "⚠️ нужна работа").append("\n");
                msg.append("   _Идеал: 1.50-1.70_\n");
                msg.append("• Грудь/Талия: ").append(String.format("%.2f", cwr)).append(" ").append(cwr >= 1.20 && cwr <= 1.35 ? "✅ отлично!" : "⚠️ нужна работа").append("\n");
                msg.append("   _Идеал: 1.20-1.35_\n\n");

                msg.append("🎯 *ПЕРСОНАЛЬНЫЕ РЕКОМЕНДАЦИИ:*\n\n");
                if (swr < 1.5) {
                    msg.append("📌 *Плечи слишком узкие:*\n");
                    msg.append("   • Жим штанги стоя 4x8\n");
                    msg.append("   • Махи гантелями в стороны 3x12\n");
                    msg.append("   • Подтягивания широким хватом 4x10\n\n");
                }
                if (cwr < 1.2) {
                    msg.append("📌 *Грудь нужно увеличить:*\n");
                    msg.append("   • Жим лёжа 4x8\n");
                    msg.append("   • Разведение гантелей 3x12\n");
                    msg.append("   • Отжимания на брусьях 3x10\n\n");
                }
                if (waist > 85) {
                    msg.append("📌 *Талия широкая — нужна сушка:*\n");
                    msg.append("   • Кардио 30 мин 3-4 раза/неделю\n");
                    msg.append("   • Дефицит калорий 300-500 ккал\n");
                    msg.append("   • Планка и вакуум каждый день\n\n");
                }
                if (bmi > 25) {
                    msg.append("📌 *Вес выше нормы:*\n");
                    msg.append("   • Калории: ").append(String.format("%.0f", weight * 25)).append(" ккал/день\n");
                    msg.append("   • Белок: ").append(String.format("%.0f", weight * 1.8)).append("г/день\n");
                    msg.append("   • Углеводы: сократить на 30%\n\n");
                } else if (bmi < 18.5) {
                    msg.append("📌 *Вес ниже нормы:*\n");
                    msg.append("   • Калории: ").append(String.format("%.0f", weight * 35)).append(" ккал/день\n");
                    msg.append("   • Белок: ").append(String.format("%.0f", weight * 2.0)).append("г/день\n");
                    msg.append("   • 5-6 приёмов пищи в день\n\n");
                }
            } else {
                msg.append("📏 *ТВОИ ПАРАМЕТРЫ:*\n");
                msg.append("• Рост: ").append(String.format("%.0f", height)).append(" см ").append(height >= 162 && height <= 175 ? "✅ идеально" : "⚠️").append("\n");
                msg.append("• Вес: ").append(String.format("%.1f", weight)).append(" кг\n");
                msg.append("• BMI: ").append(String.format("%.1f", bmi)).append(" ").append(bmi >= 19.0 && bmi <= 24.0 ? "✅ норма" : bmi < 19 ? "⚠️ недовес" : "⚠️ перевес").append("\n");
                msg.append("• Грудь: ").append(String.format("%.0f", chest)).append(" см ").append(chest >= 85 && chest <= 95 ? "✅" : "⚠️").append("\n");
                msg.append("• Талия: ").append(String.format("%.0f", waist)).append(" см\n");
                msg.append("• Бёдра: ").append(String.format("%.0f", hips)).append(" см\n\n");

                double whr = waist / hips;
                double cwr = chest / waist;
                msg.append("✨ *КЛЮЧЕВЫЕ ПРОПОРЦИИ:*\n");
                msg.append("• Талия/Бёдра: ").append(String.format("%.2f", whr)).append(" ").append(whr >= 0.65 && whr <= 0.75 ? "✅ песочные часы!" : "⚠️ нужна работа").append("\n");
                msg.append("   _Идеал: 0.65-0.75_\n");
                msg.append("• Грудь/Талия: ").append(String.format("%.2f", cwr)).append(" ").append(cwr >= 0.85 && cwr <= 0.95 ? "✅ отлично!" : "⚠️ нужна работа").append("\n");
                msg.append("   _Идеал: 0.85-0.95_\n\n");

                msg.append("🎯 *ПЕРСОНАЛЬНЫЕ РЕКОМЕНДАЦИИ:*\n\n");
                if (whr > 0.75) {
                    msg.append("📌 *Талия широкая:*\n");
                    msg.append("   • Кардио 30 мин 3-4 раза/неделю\n");
                    msg.append("   • Дефицит калорий 200-400 ккал\n");
                    msg.append("   • Вакуум живота каждое утро\n\n");
                }
                if (hips < 90) {
                    msg.append("📌 *Бёдра нужно увеличить:*\n");
                    msg.append("   • Приседания 4x12\n");
                    msg.append("   • Выпады с гантелями 3x10\n");
                    msg.append("   • Ягодичный мостик 4x15\n\n");
                }
                if (bmi > 24) {
                    msg.append("📌 *Рекомендации по весу:*\n");
                    msg.append("   • Калории: ").append(String.format("%.0f", weight * 23)).append(" ккал/день\n");
                    msg.append("   • Белок: ").append(String.format("%.0f", weight * 1.5)).append("г/день\n");
                    msg.append("   • Больше овощей и клетчатки\n\n");
                }
            }

            msg.append("📅 *ПЛАН НА НЕДЕЛЮ:*\n");
            msg.append("Пн — Силовая (верх тела)\n");
            msg.append("Вт — Кардио 30 мин\n");
            msg.append("Ср — Силовая (низ тела)\n");
            msg.append("Чт — Отдых\n");
            msg.append("Пт — Полная тренировка\n");
            msg.append("Сб — Кардио + растяжка\n");
            msg.append("Вс — Отдых\n");

            msg.append("\n═══════════════════════════════\n");
        }

        return msg.toString();
    }
    public static String compareToCelebrity(double rating, String gender) {
        String[] celebs = gender.equals("male") ? new String[]{"Генри Кавилл (9.5)","Тимоти Шаламе (8.0)"} : new String[]{"Марго Робби (9.5)","Зендая (8.5)"};
        String celeb = celebs[(int)(Math.random()*celebs.length)];
        return "⭐ *Сравнение:*\nВаш рейтинг: "+String.format("%.1f",rating)+"\nЗнаменитость: "+celeb;
    }
}
