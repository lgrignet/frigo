import nodemailer from 'nodemailer';

/**
 * Transport nodemailer basé sur le binaire `sendmail` local (Postfix déjà en
 * place sur le VPS). Pas de connexion SMTP réseau, pas d'identifiants.
 */
const transport = nodemailer.createTransport({
    sendmail: true,
    newline: 'unix',
    path: '/usr/sbin/sendmail',
});

const MAIL_FROM = process.env.MAIL_FROM || 'noreply@noshi.be';

/**
 * Envoie un email de vérification en texte brut.
 * @param {string} email destinataire
 * @param {string} code code à 6 chiffres
 */
export async function sendVerificationEmail(email, code) {
    await transport.sendMail({
        from: `MyStockManager <${MAIL_FROM}>`,
        to: email,
        subject: 'Votre code de vérification MyStockManager',
        text:
            `Bonjour,\n\n` +
            `Votre code de vérification est : ${code}\n\n` +
            `Ce code est valable 15 minutes.\n\n` +
            `Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.\n\n` +
            `— MyStockManager`,
    });
}
